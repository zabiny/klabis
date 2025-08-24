package club.klabis.members.infrastructure.restapi.mappers;

import club.klabis.members.domain.Member;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import club.klabis.members.infrastructure.restapi.dto.LicencesApiDto;
import club.klabis.members.infrastructure.restapi.dto.MemberApiDto;
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

        MemberApiDto item = conversionService.convert(m,
                club.klabis.members.infrastructure.restapi.dto.MemberApiDto.class);

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