package club.klabis.tests.common;

import club.klabis.shared.config.mapstruct.ConversionServiceAdapter;
import club.klabis.shared.config.mapstruct.MapperSpringConfig;
import org.mapstruct.extensions.spring.test.ConverterScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@ConverterScan(basePackageClasses = {MapperSpringConfig.class})
@Import(ConversionServiceAdapter.class)
public class MapperTestConfiguration {
}
