package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.core.EntityInformation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InMemoryEntityStore {

    private final Map<Class<?>, Map<Object, Object>> entitiesByClass = new ConcurrentHashMap<>();
    private final Map<Class<?>, EntityInformation<?, ?>> entityInformationMap = new ConcurrentHashMap<>();

    public <T, ID> void register(Class<T> entityClass, EntityInformation<T, ID> entityInformation) {
        entitiesByClass.putIfAbsent(entityClass, new ConcurrentHashMap<>());
        entityInformationMap.put(entityClass, entityInformation);
    }

    @SuppressWarnings("unchecked")
    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        Map<Object, Object> entities = entitiesByClass.get(entityClass);
        if (entities == null) {
            return Optional.empty();
        }

        return Optional.ofNullable((T) entities.get(id));
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> entityClass) {
        Map<Object, Object> entities = entitiesByClass.get(entityClass);
        if (entities == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>((Collection<T>) entities.values());
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
        Map<Object, Object> entities = entitiesByClass.computeIfAbsent(entityClass, k -> new HashMap<>());

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

        entities.put(id, entity);
        return entity;
    }

    @SuppressWarnings("unchecked")
    public <T, ID> void delete(T entity) {
        Class<?> entityClass = entity.getClass();
        Map<Object, Object> entities = entitiesByClass.get(entityClass);
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
        Map<Object, Object> entities = entitiesByClass.get(entityClass);
        if (entities == null) {
            return;
        }

        entities.remove(id);
    }
}