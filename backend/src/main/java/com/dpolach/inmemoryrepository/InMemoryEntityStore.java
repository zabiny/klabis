package com.dpolach.inmemoryrepository;

import com.nimbusds.jose.shaded.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.EntityInformation;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InMemoryEntityStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEntityStore.class);
    private final Map<Class<?>, Map<Object, Object>> entitiesByClass;
    private final Collection<InMemoryEntityInformation<?, ?>> entityInformationMap;

    // at this moment we clone using GSON by serialize and deserialize to/from JSON which creates new instances with same contents. But any better way of deep cloning shall be fine here.
    private EntityCloner entityCloner = new GsonCloner();

    public InMemoryEntityStore() {
        this(new HashMap<>(), new ArrayList<>());
    }

    private Optional<Map<Object, Object>> getEntityStore(Class<?> entityClass) {
        var entityInformation = getEntityInformation(entityClass);
        return Optional.ofNullable(entitiesByClass.get(entityInformation.getJavaType()));
    }

    private <ID, VALUE> Map<ID, VALUE> cloneValues(Map<ID, VALUE> original) {
        Map<ID, VALUE> clone = new HashMap<>();
        original.forEach((key, value) -> {
            clone.put(key, entityCloner.deepClone(value));
        });
        return clone;
    }

    private synchronized void setDataFrom(Map<Class<?>, Map<Object, Object>> data) {
        entitiesByClass.clear();
        data.forEach(
                (key, value) -> entitiesByClass.put(key, Collections.synchronizedMap(cloneValues(value))));
    }

    private InMemoryEntityStore(
            Map<Class<?>, Map<Object, Object>> entitiesByClass,
            Collection<InMemoryEntityInformation<?, ?>> entityInformationMap
    ) {
        this.entitiesByClass = Collections.synchronizedMap(new HashMap<>());
        setDataFrom(entitiesByClass);
        this.entityInformationMap = new ArrayList<>(entityInformationMap);
    }

    protected InMemoryEntityStore backupClone() {
        return new InMemoryEntityStore(entitiesByClass, entityInformationMap);
    }

    private String describeStoredData(Map<Class<?>, Map<Object, Object>> entitiesByClass) {
        return entitiesByClass.entrySet().stream()
                .map(e -> {
                    String entityName = e.getKey().getSimpleName();
                    return String.format("%s with %d entities [%s]",
                            entityName,
                            e.getValue().size(),
                            e.getValue()
                                    .entrySet()
                                    .stream()
                                    .map(e1 -> "%s->%s".formatted(e1.getKey(), e1.getValue()))
                                    .collect(Collectors.joining()));
                })
                .collect(Collectors.joining("\n\t", "\n\t", ""));
    }

    public void deleteAllData() {
        this.entitiesByClass.clear();
    }

    protected void restoreFromClone(InMemoryEntityStore backup) {
        if (log.isTraceEnabled()) {
            log.trace("Current store: {}", describeStoredData(entitiesByClass));
            log.trace("Restoring to {}", describeStoredData(backup.entitiesByClass));
        }

        this.setDataFrom(backup.entitiesByClass);

        entityInformationMap.clear();
        entityInformationMap.addAll(backup.entityInformationMap);
    }

    <T, ID> void register(InMemoryEntityInformation<T, ID> entityInformation) {
        var rootEntityType = entityInformation.getJavaType();
        entitiesByClass.putIfAbsent(rootEntityType, new ConcurrentHashMap<>());
        entityInformationMap.add(entityInformation);
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        return (Optional<T>) getEntityStore(entityClass)
                .flatMap(store -> Optional.of(store.get(id)))
                .map(value -> entityCloner.deepClone(value));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> entityClass) {
        return (List<T>) getEntityStore(entityClass).map(entities -> entities.values()
                        .stream()
                        .map(s -> entityCloner.deepClone(s))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .orElseGet(ArrayList::new);
    }

    public <T> Page<T> findAll(Class<T> entityClass, Pageable pageable, Predicate<T> predicate) {
        List<T> allData = findAll(entityClass, predicate).stream()
                .sorted(new SortComparator<>(pageable.getSort()))
                .toList();

        return PageUtils.create(allData, pageable);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> entityClass, Predicate<T> predicate) {
        List<T> allEntities = findAll(entityClass);
        return allEntities.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> findOne(Class<T> entityClass, Predicate<T> predicate) {
        List<T> allEntities = findAll(entityClass);
        return allEntities.stream()
                .filter(predicate)
                .findFirst();
    }

    private <T> InMemoryEntityInformation<T, Object> getEntityInformation(Class<T> entityClass) {
        return (InMemoryEntityInformation<T, Object>) entityInformationMap
                .stream().filter(ei -> ei.isEntity(entityClass)).findAny()
                .orElseThrow(() -> new IllegalStateException("No EntityInformation registered for " + entityClass));
    }

    @SuppressWarnings("unchecked")
    public <T> T save(T entity) {
        Class<T> entityClass = (Class<T>) entity.getClass();
        var entityInformation = getEntityInformation(entityClass);

        Map<Object, Object> entities = getEntityStore(entityInformation.getJavaType())
                .orElseGet(() -> entitiesByClass.put(entityInformation.getJavaType(), new HashMap<>()));

        Object id = entityInformation.getId(entity);
        if (id == null) {
            // Pokud je ID null, měli bychom vytvořit nové ID (pro autogenerované ID)
            // Pro jednoduchost necháváme tento případ nevyřešený)
            throw new IllegalStateException("ID cannot be null for entity: " + entity);
        }

        entities.put(id, entityCloner.deepClone(entity));
        return entity;
    }

    @SuppressWarnings("unchecked")
    public <T> void delete(T entity) {
        Class<T> entityClass = (Class<T>) entity.getClass();
        EntityInformation<T, Object> entityInformation = getEntityInformation(entityClass);

        if (entityInformation == null) {
            throw new IllegalStateException("No EntityInformation registered for " + entityClass);
        }

        getEntityStore(entityClass).ifPresent(entities -> {
            Object id = entityInformation.getId(entity);
            if (id != null) {
                entities.remove(id);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T, ID> void deleteById(Class<T> entityClass, ID id) {
        getEntityStore(entityClass)
                .ifPresent(store -> store.remove(id));
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
            .setPrettyPrinting()
            .create();


    @Override
    public <T> T deepClone(T original) {
        Class<T> type = (Class<T>) original.getClass();
        return gson.fromJson(gson.toJson(original), type);
    }
}