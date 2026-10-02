package com.klabis.groups;

import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the ports required by the web beans of the groups module (free group and training group
 * controllers, exception handlers, member link processor) so that other modules' {@code @WebMvcTest}
 * slices can load groups web beans without its application layer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        FreeGroupManagementPort.class,
        TrainingGroupManagementPort.class
})
public @interface GroupsWebMvcMockitoBeans {
}
