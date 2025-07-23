package club.klabis;

import club.klabis.api.dto.SexApiDto;
import club.klabis.application.events.EventCreationUseCase;
import club.klabis.application.members.MemberRegistrationUseCase;
import club.klabis.application.members.MembershipSuspendUseCase;
import club.klabis.domain.appusers.ApplicationGrant;
import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUserService;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import club.klabis.domain.events.Event;
import club.klabis.domain.events.forms.EventEditationForm;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@ConditionalOnProperty(name = "klabis.preset-data", havingValue = "true", matchIfMissing = true)
@Component
public class PresetDataLoader implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PresetDataLoader.class);

    private final ApplicationUsersRepository appUsersRepository;
    private final MembershipSuspendUseCase membershipSuspendUseCase;
    private final MemberRegistrationUseCase memberRegistrationUseCase;
    private final ApplicationUserService applicationUserService;
    private final ConversionService conversionService;
    private final EventCreationUseCase eventsService;

    public PresetDataLoader(ApplicationUsersRepository appUsersRepository, MembershipSuspendUseCase membershipSuspendUseCase, MemberRegistrationUseCase memberRegistrationUseCase, ApplicationUserService applicationUserService, ConversionService conversionService, EventCreationUseCase eventsService) {
        this.appUsersRepository = appUsersRepository;
        this.membershipSuspendUseCase = membershipSuspendUseCase;
        this.memberRegistrationUseCase = memberRegistrationUseCase;
        this.applicationUserService = applicationUserService;
        this.conversionService = conversionService;
        this.eventsService = eventsService;
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
            Member registeredMember = memberRegistrationUseCase.registerMember(csvLine.getRegistration(conversionService));
            if (csvLine.disabled()) {
                membershipSuspendUseCase.suspendMembershipForMember(registeredMember.getId(), true);
            }
            applicationUserService.setGlobalGrants(registeredMember.getId(), EnumSet.allOf(ApplicationGrant.class));
            csvLine.getGoogleId().ifPresent(googleId -> applicationUserService.linkWithGoogleId(csvLine.registrationNumber(), googleId));
        });

        // ... some additional data?
        Event createdEvent = eventsService.createNewEvent(new EventEditationForm("Example opened event", "Brno", LocalDate.now(), "ZBM", LocalDate.now()
                .plusDays(3), null));
        System.out.printf("Created event with ID %s%n", createdEvent.getId());
        createdEvent = eventsService.createNewEvent(new EventEditationForm("Example passed event", "Jilemnice", LocalDate.now().minusDays(12), "ZBM", LocalDate.now()
                .minusDays(20), null));
        System.out.printf("Created event with ID %s%n", createdEvent.getId());
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
            String googleId,
            boolean disabled
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
