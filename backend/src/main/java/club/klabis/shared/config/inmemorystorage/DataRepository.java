package club.klabis.shared.config.inmemorystorage;

import java.util.Optional;

public interface DataRepository<T, ID> {

    T save(T event);

    Optional<T> findById(ID event);

}
