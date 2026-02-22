package com.klabis;

import com.klabis.members.MemberCreatedEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for Klabis Backend API.
 * <p>
 * Klabis is a HATEOAS-compliant REST API for orienteering club management,
 * providing member registration, event management, and financial tracking.
 *
 * <p>This application uses Spring Modulith to implement a modular monolith architecture
 * with event-driven communication between bounded contexts (Members, Users, Events, Finances).
 *
 * <p>The {@link Modulithic} annotation enables Spring Modulith's modular architecture features:
 * <ul>
 *   <li>Automatic module detection from package structure</li>
 *   <li>Event publication with transactional outbox pattern</li>
 *   <li>Guaranteed at-least-once event delivery</li>
 *   <li>Async event processing between modules</li>
 * </ul>
 *
 * <p>Module structure:
 * <ul>
 *   <li><b>members</b> - Member domain (aggregate root, registration, personal information)</li>
 *   <li><b>users</b> - User management (authentication, authorization, password setup)</li>
 *   <li><b>events</b> - Event organization (races, trainings, competitions)</li>
 *   <li><b>calendar</b> - Calendar management (event-linked and manual calendar items)</li>
 *   <li><b>finances</b> - Financial management (fees, payments, accounting)</li>
 *   <li><b>config</b> - Shared configuration and infrastructure</li>
 * </ul>
 *
 * <p>Modules communicate via domain events (e.g., {@link MemberCreatedEvent})
 * using the transactional outbox pattern for guaranteed delivery.
 */
@Modulithic(
        systemName = "Klabis Membership Management",
        sharedModules = {"common", "users"}  // Always included in module tests (users because of UserDetailsService implementation)
)
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
public class KlabisApplication {

    public static void main(String[] args) {
        SpringApplication.run(KlabisApplication.class, args);
    }
}
