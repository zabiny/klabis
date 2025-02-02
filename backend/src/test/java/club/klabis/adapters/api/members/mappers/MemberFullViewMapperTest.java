package club.klabis.adapters.api.members.mappers;

import club.klabis.adapters.api.KlabisSecurityService;
import club.klabis.common.ConversionServiceAdapter;
import club.klabis.common.mapstruct.MapperSpringConfig;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.forms.RegistrationFormBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.extensions.spring.test.ConverterScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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