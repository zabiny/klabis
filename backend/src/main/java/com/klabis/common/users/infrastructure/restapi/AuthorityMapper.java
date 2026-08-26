package com.klabis.common.users.infrastructure.restapi;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL
)
public interface AuthorityMapper {

    Authority toDto(com.klabis.common.users.Authority authority);

    com.klabis.common.users.Authority toDomain(Authority authority);

    Set<Authority> toDtoSet(Set<com.klabis.common.users.Authority> authorities);

    Set<com.klabis.common.users.Authority> toDomainSet(Set<Authority> authorities);
}
