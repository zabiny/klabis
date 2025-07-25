package club.klabis.members.adapters.restapi.mappers;

import club.klabis.api.dto.LicencesApiDto;
import club.klabis.api.dto.MemberApiDto;
import club.klabis.members.domain.Member;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.tests.common.MapperTest;
import org.junit.jupiter.api.Test;
import org.mapstruct.extensions.spring.test.ConverterScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

@MapperTest
@ConverterScan(basePackageClasses = MemberFullViewMapper.class)
class MemberFullViewMapperTest {

    @Autowired
    ConversionService conversionService;

    @MockBean
    KlabisSecurityService securityServiceMock;

    @Test
    void convert() {
        var form = RegistrationFormBuilder.builder().firstName("Test").lastName("Something").build();
        Member m = Member.fromRegistration(form);

        club.klabis.api.dto.MemberApiDto item = conversionService.convert(m, club.klabis.api.dto.MemberApiDto.class);

        MemberApiDto expected = new MemberApiDto();
        expected.setFirstName("Test");
        expected.setLastName("Something");
        expected.setLicences(new LicencesApiDto());
        expected.setMedicCourse(false);

        assertThat(item)
                .as("Unexpected values")
                .usingRecursiveComparison()
                .ignoringFields("links", "id")
                .isEqualTo(expected);

    }
}