package club.klabis.members.adapters.restapi.mappers;

import club.klabis.config.security.KlabisSecurityService;
import club.klabis.shared.ConversionServiceAdapter;
import club.klabis.config.mapstruct.MapperSpringConfig;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.extensions.spring.test.ConverterScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringJUnitConfig
@Import(KlabisSecurityService.class)
class MemberFullViewMapperTest {

    @TestConfiguration
    @ConverterScan(basePackageClasses = {MapperSpringConfig.class, MemberFullViewMapper.class, ConversionServiceAdapter.class})
    static class ScanConfiguration {}

    @Autowired
    ConversionService conversionService;

    @Test
    void convert() {
        var form = RegistrationFormBuilder.builder().firstName("Test").lastName("Something").build();
        Member m = Member.fromRegistration(form);

        club.klabis.api.dto.MemberApiDto item = conversionService.convert(m, club.klabis.api.dto.MemberApiDto.class);

        Assertions.assertAll("Unexpected values",
                () -> assertEquals("Test", item.getFirstName(), "firstName"),
                () -> assertEquals("Something", item.getLastName(), "lastName"),
                () -> assertNull(item.getRegistrationNumber(), "registrationNumber")
                // TODO: rest of attributes
                );


    }
}