package club.klabis.adapters.inmemorystorage;

import java.util.List;
import java.util.Optional;

public interface InMemoryRepository<T, ID> {
    <S extends T> S save(S entity);

    Optional<T> findById(ID id);

    boolean existsById(ID id);

    long count();

    void delete(T entity);

    void deleteAll(Iterable<? extends T> entities);

    <S extends T> List<S> saveAll(Iterable<S> entities);

    List<T> findAll();

    List<T> findAllById(Iterable<ID> ids);

}
