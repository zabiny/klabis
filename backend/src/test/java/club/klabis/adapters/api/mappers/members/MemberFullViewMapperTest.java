package club.klabis.adapters.api.mappers.members;

import club.klabis.common.MapperSpringConfig;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.forms.RegistrationFormBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.extensions.spring.test.ConverterScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@ExtendWith(SpringExtension.class)
class MemberFullViewMapperTest {

    @Configuration
    @ConverterScan(basePackageClasses = {MapperSpringConfig.class, MemberFullViewMapperImpl.class})
    static class ScanConfiguration {}

    @Autowired
    ConversionService conversionService;

//    @Test
    void convert() {
        var form = RegistrationFormBuilder.builder().firstName("Test").lastName("Something").contact(List.of())
                .guardians(List.of()).build();
        Member m = Member.fromRegistration(form);
        club.klabis.api.dto.MemberApiDto item = conversionService.convert(m, club.klabis.api.dto.MemberApiDto.class);

        Assertions.assertAll("Unexpected values",
                () -> Assertions.assertEquals("Test", item.getFirstName(), "firstName"),
                () -> Assertions.assertEquals("Test", item.getFirstName(), "lastName")
                );


    }
}