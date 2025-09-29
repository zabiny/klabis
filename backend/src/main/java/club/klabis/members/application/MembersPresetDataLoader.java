package club.klabis.members.application;

import club.klabis.PresetDataLoader;
import club.klabis.members.domain.*;
import club.klabis.members.domain.forms.RegistrationForm;
import club.klabis.shared.ConversionService;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.application.LinkWithSocialIdUseCase;
import club.klabis.users.application.UserGrantsUpdateUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@ConditionalOnProperty(name = "klabis.preset-data", havingValue = "true", matchIfMissing = true)
@Component
public class MembersPresetDataLoader implements PresetDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MembersPresetDataLoader.class);

    private final UserGrantsUpdateUseCase userGrantsUpdateUseCase;
    private final MembershipSuspendUseCase membershipSuspendUseCase;
    private final MemberRegistrationUseCase memberRegistrationUseCase;
    private final LinkWithSocialIdUseCase linkWithSocialIdUseCase;
    private final ConversionService conversionService;

    public MembersPresetDataLoader(UserGrantsUpdateUseCase userGrantsUpdateUseCase, MembershipSuspendUseCase membershipSuspendUseCase, MemberRegistrationUseCase memberRegistrationUseCase, LinkWithSocialIdUseCase linkWithSocialIdUseCase, ConversionService conversionService) {
        this.userGrantsUpdateUseCase = userGrantsUpdateUseCase;
        this.membershipSuspendUseCase = membershipSuspendUseCase;
        this.memberRegistrationUseCase = memberRegistrationUseCase;
        this.linkWithSocialIdUseCase = linkWithSocialIdUseCase;
        this.conversionService = conversionService;
    }

    @Override
    public void loadData() throws IOException {
        loadObjectList(MembersCsvLine.class, new ClassPathResource("presetData/members.csv")).forEach(csvLine -> {
            Member registeredMember = memberRegistrationUseCase.registerMember(csvLine.getRegistration(conversionService));
            if (csvLine.disabled()) {
                membershipSuspendUseCase.suspendMembershipForMember(registeredMember.getId(), true);
            }
            if (csvLine.admin()) {
                userGrantsUpdateUseCase.setGlobalGrants(registeredMember.getId(),
                        EnumSet.allOf(ApplicationGrant.class));
            }
            csvLine.getGoogleId()
                    .ifPresent(googleId -> linkWithSocialIdUseCase.linkWithGoogleId(csvLine.registrationNumber(),
                            googleId));
        });
    }

    record MembersCsvLine(
            String firstName,
            String lastName,
            RegistrationNumber registrationNumber,
            Sex sex,
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
            boolean disabled,
            boolean admin
    ) {

        public RegistrationForm getRegistration(ConversionService conversionService) {
            Address address = new Address(street, city, postalCode, country);
            Contact emailContact = new Contact(Contact.Type.EMAIL, email, null);
            Contact phoneContact = new Contact(Contact.Type.PHONE, phone, null);
            return new RegistrationForm(
                    firstName,
                    lastName,
                    conversionService.convert(sex, Sex.class),
                    conversionService.convert(dateOfBirth, LocalDate.class),
                    birthCertificateNumber,
                    nationality,
                    address,
                    List.of(emailContact, phoneContact),
                    List.of(),
                    siCard,
                    bankAccount,
                    registrationNumber,
                    orisId
            );
        }

        public Optional<String> getGoogleId() {
            return Optional.ofNullable(googleId);
        }

    }
}
