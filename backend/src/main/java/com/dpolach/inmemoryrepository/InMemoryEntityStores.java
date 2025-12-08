package com.dpolach.inmemoryrepository;

import club.klabis.KlabisApplication;
import com.nimbusds.jose.shaded.gson.*;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InMemoryEntityStores {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEntityStores.class);
    private final Collection<EntityStore<Object, Object>> stores;

    public InMemoryEntityStores() {
        this(new ArrayList<>());
    }

    private InMemoryEntityStores(Collection<EntityStore<Object, Object>> stores) {
        this.stores = stores.stream().map(EntityStore::clone).collect(Collectors.toCollection(ArrayList::new));
    }

    private <T, ID> Optional<EntityStore<T, ID>> getEntityStore(Class<T> entityClass) {
        return stores.stream().filter(store -> store.handlesEntity(entityClass))
                .sorted(EntityStore.mostSpecificToMostGenericStore())
                .map(store -> (EntityStore<T, ID>) store)
                .findFirst();
    }

    protected InMemoryEntityStores backupClone() {
        return new InMemoryEntityStores(stores);
    }

    private String describeStoredData(boolean includingData) {
        return stores.stream()
                .map(store -> store.describeStoredData(includingData))
                .collect(Collectors.joining("\n======\n\t", "Entity stores: \n\t", ""));
    }

    public void deleteAllData() {
        this.stores.forEach(EntityStore::clear);
    }

    protected void restoreFromClone(InMemoryEntityStores backup) {
        if (log.isTraceEnabled()) {
            log.trace("Current store: {}", describeStoredData(false));
            log.trace("Restoring to {}", backup.describeStoredData(false));
        }

        this.stores.clear();
        this.stores.addAll(backup.stores.stream().map(EntityStore::clone).toList());
    }

    void register(InMemoryEntityInformation<Object, Object> entityInformation) {
        stores.add(new EntityStore<>(new HashMap<>(), entityInformation));
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        return getEntityStore(entityClass).flatMap(store -> store.findById(id));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> entityClass) {
        return getEntityStore(entityClass).map(EntityStore::findAll).orElseGet(ArrayList::new);
    }

    public <T> Page<T> findAll(Class<T> entityClass, Pageable pageable, Predicate<T> predicate) {
        return getEntityStore(entityClass)
                .map(store -> store.findAll(pageable, predicate))
                .orElseGet(Page::empty);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> entityClass, Predicate<T> predicate) {
        return getEntityStore(entityClass).map(store -> store.findAll(predicate)).orElseGet(ArrayList::new);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> findOne(Class<T> entityClass, Predicate<T> predicate) {
        return getEntityStore(entityClass).flatMap(store -> store.findOne(predicate));
    }

    @SuppressWarnings("unchecked")
    public <T> T save(T entity) {
        Class<T> entityClass = (Class<T>) entity.getClass();

        EntityStore<T, ?> store = getEntityStore(entityClass)
                .orElseThrow(() -> new IllegalStateException("Unknown entity type %s".formatted(entityClass)));

        return store.save(entity);
    }

    @SuppressWarnings("unchecked")
    public <T> void delete(T entity) {
        Class<T> entityClass = (Class<T>) entity.getClass();

        EntityStore<T, ?> store = getEntityStore(entityClass)
                .orElseThrow(() -> new IllegalStateException("Unknown entity type %s".formatted(entityClass)));

        store.delete(entity);
    }

    @SuppressWarnings("unchecked")
    public <T, ID> void deleteById(Class<T> entityClass, ID id) {
        EntityStore<T, ID> store = (EntityStore<T, ID>) getEntityStore(entityClass)
                .orElseThrow(() -> new IllegalStateException("Unknown entity type %s".formatted(entityClass)));

        store.deleteById(id);
    }
}

class EntityStore<T, ID> {
    private final Map<ID, T> entities;
    private final InMemoryEntityInformation<T, ID> entityInformation;
    private final EntityCloner entityCloner = new GsonCloner();

    EntityStore(Map<ID, T> entities, InMemoryEntityInformation<T, ID> entityInformation) {
        this.entities = entities;
        this.entityInformation = entityInformation;
    }

    public static Comparator<EntityStore<?, ?>> mostSpecificToMostGenericStore() {
        return new Comparator<>() {
            @Override
            public int compare(EntityStore<?, ?> o1, EntityStore<?, ?> o2) {
                return compare(o1.entityInformation.getJavaType(), o2.entityInformation.getJavaType());
            }

            private int compare(Class<?> c1, Class<?> c2) {
                if (c1.equals(c2)) {
                    return 0;
                } else if (c1.isAssignableFrom(c2)) {
                    return 1;
                } else if (c2.isAssignableFrom(c1)) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };
    }

    public boolean handlesEntity(Class<?> entityClass) {
        return entityInformation.getJavaType().isAssignableFrom(entityClass);
    }

    public EntityStore<T, ID> clone() {
        Map<ID, T> entitiesClone = new HashMap<>();
        entities.entrySet().forEach(entry -> {
            entitiesClone.put(entry.getKey(), entityCloner.deepClone(entry.getValue()));
        });
        return new EntityStore<>(entitiesClone, entityInformation);
    }

    public Optional<T> findById(ID id) {
        return Optional.of(entities.get(id))
                .map(entityCloner::deepClone);
    }


    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        return entities.values()
                .stream()
                .map(s -> entityCloner.deepClone(s))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Page<T> findAll(Pageable pageable, Predicate<T> predicate) {
        List<T> allData = findAll(predicate).stream()
                .sorted(new SortComparator<>(pageable.getSort()))
                .toList();

        return PageUtils.create(allData, pageable);
    }

    @SuppressWarnings("unchecked")
    public List<T> findAll(Predicate<T> predicate) {
        List<T> allEntities = findAll();
        return allEntities.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Optional<T> findOne(Predicate<T> predicate) {
        List<T> allEntities = findAll();
        return allEntities.stream()
                .filter(predicate)
                .findFirst();
    }


    @SuppressWarnings("unchecked")
    public T save(T entity) {
        ID id = entityInformation.getId(entity);
        if (id == null) {
            // Pokud je ID null, měli bychom vytvořit nové ID (pro autogenerované ID)
            // Pro jednoduchost necháváme tento případ nevyřešený)
            throw new IllegalStateException("ID cannot be null for entity: " + entity);
        }
        if (entity instanceof AbstractAggregateRoot<?> aggregateRoot) {
            //aggregateRoot.
        }
        entities.put(id, entityCloner.deepClone(entity));
        return entity;
    }

    @SuppressWarnings("unchecked")
    public void delete(T entity) {
        ID id = entityInformation.getId(entity);
        if (id != null) {
            entities.remove(id);
        }
    }

    @SuppressWarnings("unchecked")
    public void deleteById(ID id) {
        entities.remove(id);
    }

    public void clear() {
        entities.clear();
    }

    protected String describeStoredData(boolean includingData) {
        String dataDescription = includingData ? " with data %s\n".formatted(entities.entrySet().stream()
                .map(e1 -> "%s->%s".formatted(e1.getKey(), e1.getValue()))
                .collect(Collectors.joining("\n\t", "\n\t", ""))) : "";

        return "Entity %s (%d)%s".formatted(entityInformation.getJavaType(),
                entities.size(),
                dataDescription).formatted();
    }

}

// create GSON adapter for java 9 LocalDate
class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    @Override
    public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(date.toString());
    }

    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonNull()) {
            return null;
        } else if (!json.isJsonPrimitive()) {
            throw new JsonParseException(json + " is not a json primitive");
        }
        try {
            return LocalDate.parse(json.getAsString());
        } catch (DateTimeException e) {
            throw new JsonParseException("could not parse date '" + json.getAsString() + "'", e);
        }
    }
}

// GSON adapter for java.time.LocalDateTime
class LocalDateTimeAdapter implements JsonSerializer<java.time.LocalDateTime>, JsonDeserializer<java.time.LocalDateTime> {
    @Override
    public JsonElement serialize(java.time.LocalDateTime dateTime, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(dateTime.toString());
    }

    @Override
    public java.time.LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonNull()) {
            return null;
        } else if (!json.isJsonPrimitive()) {
            throw new JsonParseException(json + " is not a json primitive");
        }
        try {
            return java.time.LocalDateTime.parse(json.getAsString());
        } catch (DateTimeException e) {
            throw new JsonParseException("could not parse datetime '" + json.getAsString() + "'", e);
        }
    }
}

// GSON adapter for java.time.ZonedDateTime
class ZonedDateTimeAdapter implements JsonSerializer<java.time.ZonedDateTime>, JsonDeserializer<java.time.ZonedDateTime> {
    @Override
    public JsonElement serialize(java.time.ZonedDateTime zonedDateTime, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(zonedDateTime.toString());
    }

    @Override
    public java.time.ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonNull()) {
            return null;
        } else if (!json.isJsonPrimitive()) {
            throw new JsonParseException(json + " is not a json primitive");
        }
        try {
            return java.time.ZonedDateTime.parse(json.getAsString());
        } catch (DateTimeException e) {
            throw new JsonParseException("could not parse zoneddatetime '" + json.getAsString() + "'", e);
        }
    }
}

// GSON adapter for java.time.Instant
class InstantAdapter implements JsonSerializer<java.time.Instant>, JsonDeserializer<java.time.Instant> {
    @Override
    public JsonElement serialize(java.time.Instant instant, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(instant.toString());
    }

    @Override
    public java.time.Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonNull()) {
            return null;
        } else if (!json.isJsonPrimitive()) {
            throw new JsonParseException(json + " is not a json primitive");
        }
        try {
            return java.time.Instant.parse(json.getAsString());
        } catch (DateTimeException e) {
            throw new JsonParseException("could not parse instant '" + json.getAsString() + "'", e);
        }
    }
}

/**
 * Purpose is to clone entities to avoid returning same instance to multiple "threads".
 */
interface EntityCloner {

    <T> T deepClone(T original);

}

class GsonCloner implements EntityCloner {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(java.time.ZonedDateTime.class, new ZonedDateTimeAdapter())
            .registerTypeAdapter(java.time.Instant.class, new InstantAdapter())
            //.disableJdkUnsafe()
            .registerTypeAdapterFactory(new UnsafeInstantiationWarningFactory())
            .setPrettyPrinting()
            .create();


    @Override
    public <T> T deepClone(T original) {
        Class<T> type = (Class<T>) original.getClass();
        return gson.fromJson(gson.toJson(original), type);
    }
}

class UnsafeInstantiationWarningFactory implements TypeAdapterFactory {

    private static final String APP_PACKAGE_NAME = KlabisApplication.class.getPackage().getName();

    private static final Logger LOG = LoggerFactory.getLogger(UnsafeInstantiationWarningFactory.class);

    private static Set<Class<?>> warnedTypes = new HashSet<>();

    private boolean isKlabisAppClass(TypeToken<?> clazz) {
        return clazz.getRawType().getPackage() != null && clazz.getRawType()
                .getPackage()
                .getName()
                .startsWith(APP_PACKAGE_NAME);
    }

    private boolean hasNoArgumentConstructor(TypeToken<?> type) {
        return Arrays.stream(type.getRawType().getDeclaredConstructors()).anyMatch(c -> c.getParameterCount() == 0);
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        if (!warnedTypes.contains(typeToken.getRawType()) && isKlabisAppClass(typeToken) && !hasNoArgumentConstructor(
                typeToken)) {
            // For example transient attribute like AggregateRoot's domainEvents collection
            // If such problems happen, easiest solution is to declare no-args constructor for problematic class
            LOG.warn(
                    "InMemory repository cloning type {} without no-args constructor - GSON will use Unsafe operation -> deserialized instance may behave incorrectly! (some attributes may have missing value, etc..)",
                    typeToken.getRawType());
        }

        warnedTypes.add(typeToken.getRawType());

        // let handle type by other factories
        return null;
    }
}