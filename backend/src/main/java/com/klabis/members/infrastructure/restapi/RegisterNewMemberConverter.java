package com.klabis.members.infrastructure.restapi;

import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BankAccountNumber;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.GuardianInformation;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Not MapStruct-generated: constructing {@link RegistrationPort.RegisterNewMember} requires
 * conditional nested object creation and domain factory calls (e.g. {@code EmailAddress.of})
 * that are not plain field-to-field mappings. Implemented as a plain {@link Converter} bean so
 * {@code @WebMvcTest} slices pick it up automatically, consistent with the other converters here.
 * <p>
 * No dependency on {@code MemberMapper} — a {@code Converter} bean is visible to every
 * {@code @WebMvcTest} slice regardless of its {@code controllers} filter, so depending on a plain,
 * non-Converter {@code @Mapper} bean would force every unrelated slice to import it too.
 */
@Component
class RegisterNewMemberConverter implements Converter<RegisterMemberRequestWithParameters, RegistrationPort.RegisterNewMember> {

    @Override
    public RegistrationPort.RegisterNewMember convert(RegisterMemberRequestWithParameters source) {
        RegisterMemberRequest request = source.request();
        com.klabis.members.domain.Gender gender = com.klabis.members.domain.Gender.valueOf(request.gender().name());
        return new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(request.firstName(), request.lastName(),
                        request.dateOfBirth(), request.nationality(), gender),
                request.address() != null ? new Address(request.address().street(), request.address().city(),
                        request.address().postalCode(), request.address().country()) : null,
                EmailAddress.of(request.email()),
                PhoneNumber.of(request.phone()),
                request.guardian() != null ? new GuardianInformation(request.guardian().firstName(),
                        request.guardian().lastName(), request.guardian().relationship(),
                        request.guardian().email(), request.guardian().phone()) : null,
                request.birthNumber() != null ? BirthNumber.of(request.birthNumber()) : null,
                request.bankAccountNumber() != null ? BankAccountNumber.of(request.bankAccountNumber()) : null,
                source.registeredBy()
        );
    }
}
