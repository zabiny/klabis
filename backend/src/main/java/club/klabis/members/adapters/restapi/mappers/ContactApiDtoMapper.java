package club.klabis.members.adapters.restapi.mappers;

import club.klabis.api.dto.ContactApiDto;
import club.klabis.config.mapstruct.DomainToDtoMapperConfiguration;
import club.klabis.members.domain.Contact;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Mapper(config = DomainToDtoMapperConfiguration.class)
interface ContactApiDtoMapper extends Converter<Collection<Contact>, ContactApiDto> {

    @Override
    default ContactApiDto convert(Collection<Contact> source) {
        Optional<Contact> emailContact = source.stream().filter(c -> Contact.Type.EMAIL.equals(c.type())).findAny();
        Optional<Contact> phoneCOntact = source.stream().filter(c -> Contact.Type.PHONE.equals(c.type())).findAny();

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
            result.add(new Contact(Contact.Type.EMAIL, apiDto.getEmail(), apiDto.getNote()));
        }
        if (StringUtils.hasLength(apiDto.getPhone())) {
            result.add(new Contact(Contact.Type.PHONE, apiDto.getPhone(), apiDto.getNote()));
        }
        return result;
    }

}
