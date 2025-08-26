package club.klabis.shared.config.ddd;

import org.jmolecules.ddd.annotation.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines use case
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Service
@org.springframework.stereotype.Service
public @interface UseCase {

}
