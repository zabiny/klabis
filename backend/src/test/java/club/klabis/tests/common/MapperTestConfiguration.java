package club.klabis.tests.common;

import club.klabis.common.mapstruct.MapperSpringConfig;
import org.mapstruct.extensions.spring.test.ConverterScan;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
@ConverterScan(basePackageClasses = MapperSpringConfig.class)
class MapperTestConfiguration {
}
