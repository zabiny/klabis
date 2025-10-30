package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Contact;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import club.klabis.members.infrastructure.restapi.dto.ContactApiDto;
import club.klabis.members.infrastructure.restapi.dto.LicencesApiDto;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponse;
import club.klabis.members.infrastructure.restapi.dto.MembersApiResponseBuilder;
import club.klabis.shared.ConversionService;
import club.klabis.tests.common.MapperTest;
import org.junit.jupiter.api.Test;
import org.mapstruct.extensions.spring.test.ConverterScan;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MapperTest
@ConverterScan(basePackageClasses = MembersApiResponseConverter.class)
class MembersApiResponseConverterTest {

    @Autowired
    ConversionService conversionService;

    @Test
    void convert() {
        var form = RegistrationFormBuilder.builder()
                .firstName("Test")
                .lastName("Something")
                .contact(List.of(new Contact(
                        Contact.Type.EMAIL, "email@com.com", "email domu")))
                .build();
        Member m = Member.fromRegistration(form);

        MembersApiResponse item = conversionService.convert(m,
                MembersApiResponse.class);

        MembersApiResponse expected = MembersApiResponseBuilder.builder()
                .id(m.getId())
                .member(m)
                .firstName("Test")
                .lastName("Something")
                .licences(new LicencesApiDto())
                .medicCourse(false)
                .contact(new ContactApiDto().email("email@com.com").note("email domu"))
                .build();

        assertThat(item)
                .as("Unexpected values")
                .usingRecursiveComparison()
                //.ignoringFields("links", "id")
                .isEqualTo(expected);

    }
}