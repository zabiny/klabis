package club.klabis.adapters.api.mappers;

import club.klabis.api.dto.ContactApiDto;
import club.klabis.domain.members.Contact;
import club.klabis.domain.members.ContactType;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper(config = DomainToApiDtoMapperConfiguration.class)
interface ContactApiDtoMapper extends Converter<Collection<Contact>, ContactApiDto> {

    @Override
    default ContactApiDto convert(Collection<Contact> source) {
        Optional<Contact> emailContact = source.stream().filter(c -> ContactType.EMAIL.equals(c.type())).findAny();
        Optional<Contact> phoneCOntact = source.stream().filter(c -> ContactType.PHONE.equals(c.type())).findAny();

        if (emailContact.isEmpty() && phoneCOntact.isEmpty()) {
            return null;
        }

        var builder = ContactApiDto.builder();
        emailContact.ifPresent(a -> builder.email(a.value()).note(a.note()));
        phoneCOntact.ifPresent(a -> builder.phone(a.value()).note(a.note()));
        return builder.build();
    }

    @DelegatingConverter
    default Collection<Contact> fromApiDto(ContactApiDto apiDto) {
        List<Contact> result = new ArrayList<>();
        if (StringUtils.hasLength(apiDto.getEmail())) {
            result.add(new Contact(ContactType.EMAIL, apiDto.getEmail(), apiDto.getNote()));
        }
        if (StringUtils.hasLength(apiDto.getPhone())) {
            result.add(new Contact(ContactType.PHONE, apiDto.getPhone(), apiDto.getNote()));
        }
        return result;
    }

}
