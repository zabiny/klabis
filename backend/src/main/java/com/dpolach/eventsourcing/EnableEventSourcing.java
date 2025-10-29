package com.dpolach.eventsourcing;

import com.dpolach.inmemoryrepository.EnableInMemoryRepositories;
import org.springframework.context.annotation.ComponentScan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ComponentScan(basePackageClasses = EnableEventSourcing.class)
@EnableInMemoryRepositories(basePackageClasses = Repo.class)
public @interface EnableEventSourcing {
}
