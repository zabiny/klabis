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
    private final Map<Class<?>, Map<Object, String>> entitiesByClass;
    private final Map<Class<?>, EntityInformation<?, ?>> entityInformationMap;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    public InMemoryEntityStore() {
        this(new HashMap<>(), new HashMap<>());
    }

    private synchronized void setDataFrom(Map<Class<?>, Map<Object, String>> data) {
        entitiesByClass.clear();
        data.forEach(
                (key, value) -> entitiesByClass.put(key, Collections.synchronizedMap(new HashMap<>(value))));
    }

    private InMemoryEntityStore(
            Map<Class<?>, Map<Object, String>> entitiesByClass,
            Map<Class<?>, EntityInformation<?, ?>> entityInformationMap
    ) {
        this.entitiesByClass = Collections.synchronizedMap(new HashMap<>());
        setDataFrom(entitiesByClass);
        this.entityInformationMap = entityInformationMap.entrySet()
                .stream()
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, e -> e.getValue()));
    }

    protected InMemoryEntityStore backupClone() {
        return new InMemoryEntityStore(entitiesByClass, entityInformationMap);
    }

    private String describeStoredData(Map<Class<?>, Map<Object, String>> entitiesByClass) {
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
        entityInformationMap.putAll(backup.entityInformationMap);
    }

    public <T, ID> void register(Class<T> entityClass, EntityInformation<T, ID> entityInformation) {
        entitiesByClass.putIfAbsent(entityClass, new ConcurrentHashMap<>());
        entityInformationMap.put(entityClass, entityInformation);
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        Map<Object, String> entities = entitiesByClass.get(entityClass);
        if (entities == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(entities.get(id))
                .map(s -> gson.fromJson(s, entityClass));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> entityClass) {
        Map<Object, String> entities = entitiesByClass.get(entityClass);
        if (entities == null) {
            return new ArrayList<>();
        }

        return entities.values()
                .stream()
                .map(s -> gson.fromJson(s, entityClass))
                .collect(Collectors.toCollection(ArrayList::new));
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

    @SuppressWarnings("unchecked")
    public <T, ID> T save(T entity) {
        Class<?> entityClass = entity.getClass();
        Map<Object, String> entities = entitiesByClass.computeIfAbsent(entityClass, k -> new HashMap<>());

        EntityInformation<T, ID> entityInformation =
                (EntityInformation<T, ID>) entityInformationMap.get(entityClass);

        if (entityInformation == null) {
            throw new IllegalStateException("No EntityInformation registered for " + entityClass);
        }

        ID id = entityInformation.getId(entity);
        if (id == null) {
            // Pokud je ID null, měli bychom vytvořit nové ID (pro autogenerované ID)
            // Pro jednoduchost necháváme tento případ nevyřešený - v reálném použití
            // byste implementovali strategii generování ID
            throw new IllegalStateException("ID cannot be null for entity: " + entity);
        }

        entities.put(id, gson.toJson(entity));
        return entity;
    }

    @SuppressWarnings("unchecked")
    public <T, ID> void delete(T entity) {
        Class<?> entityClass = entity.getClass();
        Map<Object, String> entities = entitiesByClass.get(entityClass);
        if (entities == null) {
            return;
        }

        EntityInformation<T, ID> entityInformation =
                (EntityInformation<T, ID>) entityInformationMap.get(entityClass);

        if (entityInformation == null) {
            throw new IllegalStateException("No EntityInformation registered for " + entityClass);
        }

        ID id = entityInformation.getId(entity);
        if (id != null) {
            entities.remove(id);
        }
    }

    @SuppressWarnings("unchecked")
    public <T, ID> void deleteById(Class<T> entityClass, ID id) {
        Map<Object, String> entities = entitiesByClass.get(entityClass);
        if (entities == null) {
            return;
        }

        entities.remove(id);
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
