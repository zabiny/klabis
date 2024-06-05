package club.klabis.adapters.inmemorystorage;

import org.springframework.data.repository.ListCrudRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

abstract class MapBackedRepository<E, I> implements ListCrudRepository<E, I> {

    private Map<I, E> data = new HashMap<>();

    private final Function<E, I> idExtractor;

    public MapBackedRepository(Function<E, I> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public <S extends E> S save(S entity) {
        data.put(idExtractor.apply(entity), entity);
        return entity;
    }

    @Override
    public <S extends E> List<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false).map(this::save).toList();
    }

    @Override
    public Optional<E> findById(I i) {
        return Optional.ofNullable(data.get(i));
    }

    @Override
    public boolean existsById(I i) {
        return data.containsKey(i);
    }

    @Override
    public List<E> findAll() {
        return data.values().stream().toList();
    }

    @Override
    public List<E> findAllById(Iterable<I> is) {
        return StreamSupport.stream(is.spliterator(), false).map(this::findById).flatMap(Optional::stream).toList();
    }

    @Override
    public long count() {
        return data.size();
    }

    @Override
    public void deleteById(I i) {
        data.remove(i);
    }

    @Override
    public void delete(E entity) {
        deleteById(idExtractor.apply(entity));
    }

    @Override
    public void deleteAllById(Iterable<? extends I> is) {
        is.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends E> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        data.clear();
    }
}
