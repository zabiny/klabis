package club.klabis;

import club.klabis.api.dto.SexApiDto;
import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUserService;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import club.klabis.domain.members.*;
import club.klabis.domain.members.forms.RegistrationForm;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class PresetDataLoader implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PresetDataLoader.class);

    private final ApplicationUsersRepository appUsersRepository;
    private final MemberService memberService;
    private final ApplicationUserService applicationUserService;
    private final ConversionService conversionService;

    public PresetDataLoader(ApplicationUsersRepository appUsersRepository, MemberService memberService, ApplicationUserService applicationUserService, ConversionService conversionService) {
        this.appUsersRepository = appUsersRepository;
        this.memberService = memberService;
        this.applicationUserService = applicationUserService;
        this.conversionService = conversionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // create Admin users
        appUsersRepository.findByUserName("admin").orElseGet(() -> {
            LOG.info("Adding user admin");
            ApplicationUser admin = ApplicationUser.newAppUser("admin", "{noop}secret");
            admin.setGlobalGrants(EnumSet.allOf(ApplicationGrant.class));
            return appUsersRepository.save(admin);
        });


        ClassPathResource membersFile = new ClassPathResource("presetData/members.csv");
        loadObjectList(MembersCsvLine.class, membersFile.getInputStream()).forEach(csvLine -> {
            Member registeredMember = memberService.registerMember(csvLine.getRegistration(conversionService));
            applicationUserService.setGlobalGrants(registeredMember.getId(), EnumSet.allOf(ApplicationGrant.class));
            csvLine.getGoogleId().ifPresent(googleId -> applicationUserService.linkWithGoogleId(csvLine.registrationNumber(), googleId));
        });

        // ... some additional data?
    }

    public <T> List<T> loadObjectList(Class<T> type, InputStream inputData) throws IOException {
        CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
        CsvMapper mapper = CsvMapper.builder().enable(CsvParser.Feature.EMPTY_STRING_AS_NULL).build();
        MappingIterator<T> readValues = mapper.readerFor(type).with(bootstrapSchema).readValues(inputData);
        return readValues.readAll();
    }

    record MembersCsvLine(
            String firstName,
            String lastName,
            RegistrationNumber registrationNumber,
            SexApiDto sex,
            String dateOfBirth,
            String birthCertificateNumber,
            String nationality,
            String street,
            String city,
            String postalCode,
            String country,
            String email,
            String phone,
            String siCard,
            String bankAccount,
            Integer orisId,
            String googleId
    ) {

        public RegistrationForm getRegistration(ConversionService conversionService) {
            Address address = new Address(street, city, postalCode, country);
            Contact emailContact = new Contact(Contact.Type.EMAIL, email, null);
            Contact phoneContact = new Contact(Contact.Type.PHONE, phone, null);
            return new RegistrationForm(
                    firstName, lastName, conversionService.convert(sex, Sex.class), conversionService.convert(dateOfBirth, LocalDate.class), birthCertificateNumber, nationality, address, List.of(emailContact, phoneContact),
                    List.of(),
                    siCard, bankAccount, registrationNumber, orisId
            );
        }

        public Optional<String> getGoogleId() {
            return Optional.ofNullable(googleId);
        }

    }
}
