package club.klabis.shared.config.ddd.forms;

import java.util.Collection;
import java.util.Optional;

public interface FormHandlersRegistry {
    Collection<FormApiDescriptor<?>> getFormApis();

    Optional<FormApiDescriptor<?>> findFormApiByPath(String apiPath);
}
