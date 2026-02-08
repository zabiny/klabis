package com.klabis.events.registration;

import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventRegistration;
import com.klabis.events.SiCardNumber;
import com.klabis.events.persistence.EventRepository;
import com.klabis.members.Member;
import com.klabis.members.Members;
import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for event registration operations.
 * <p>
 * Handles member registration for events including:
 * - Member registration with SI card number
 * - Duplicate registration prevention
 * - Unregistration before event date
 * - Privacy-aware registration listing
 * - Own registration retrieval
 */
@Service
@PrimaryPort
class EventRegistrationService {

    private final EventRepository eventRepository;
    private final Members members;

    public EventRegistrationService(EventRepository eventRepository, Members members) {
        this.eventRepository = eventRepository;
        this.members = members;
    }

    /**
     * Register the authenticated member for an event.
     *
     * @param eventId event ID
     * @param command registration command with SI card number
     * @throws EventNotFoundException         if event not found
     * @throws DuplicateRegistrationException if member already registered
     * @throws IllegalStateException          if event is not ACTIVE
     */
    @Transactional
    public void registerMember(UUID eventId, RegisterForEventCommand command) {
        // Get authenticated member ID
        UserId memberId = getAuthenticatedMemberId();

        // Verify user has a member record - registration requires member profile
        if (!members.findById(memberId).isPresent()) {
            throw new MemberProfileRequiredException();
        }

        // Load event
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Check for duplicate registration
        if (event.findRegistration(memberId).isPresent()) {
            throw new DuplicateRegistrationException(memberId.uuid(), eventId);
        }

        // Register member (domain logic validates ACTIVE status)
        event.registerMember(memberId, SiCardNumber.of(command.siCardNumber()));

        // Save event
        eventRepository.save(event);
    }

    /**
     * Unregister the authenticated member from an event.
     *
     * @param eventId     event ID
     * @param currentDate current date for validation
     * @throws EventNotFoundException        if event not found
     * @throws RegistrationNotFoundException if member not registered
     * @throws IllegalStateException         if current date is on or after event date
     */
    @Transactional
    public void unregisterMember(UUID eventId, LocalDate currentDate) {
        // Get authenticated member ID
        UserId memberId = getAuthenticatedMemberId();

        // Load event
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Check if member is registered
        if (event.findRegistration(memberId).isEmpty()) {
            throw new RegistrationNotFoundException(memberId.uuid(), eventId);
        }

        // Unregister member (domain logic validates date)
        event.unregisterMember(memberId, currentDate);

        // Save event
        eventRepository.save(event);
    }

    /**
     * List all registrations for an event (without SI card numbers for privacy).
     *
     * @param eventId event ID
     * @return list of registration DTOs without SI card numbers
     * @throws EventNotFoundException if event not found
     */
    @Transactional(readOnly = true)
    public List<RegistrationDto> listRegistrations(UUID eventId) {
        // Load event
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Map registrations to DTOs (without SI card numbers)
        return event.getRegistrations().stream()
                .map(this::toRegistrationDto)
                .collect(Collectors.toList());
    }

    /**
     * Get the authenticated member's own registration (with SI card number).
     *
     * @param eventId event ID
     * @return own registration DTO with SI card number
     * @throws EventNotFoundException        if event not found
     * @throws RegistrationNotFoundException if member not registered
     */
    @Transactional(readOnly = true)
    public OwnRegistrationDto getOwnRegistration(UUID eventId) {
        // Get authenticated member ID
        UserId memberId = getAuthenticatedMemberId();

        // Load event
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Find member's registration
        EventRegistration registration = event.findRegistration(memberId)
                .orElseThrow(() -> new RegistrationNotFoundException(memberId.uuid(), eventId));

        // Map to DTO with SI card number
        return toOwnRegistrationDto(registration);
    }

    // ========== Helper Methods ==========

    /**
     * Get the authenticated member's user ID from SecurityContext.
     *
     * @return authenticated member's UserId
     * @throws IllegalStateException if not authenticated or user ID is invalid
     */
    private UserId getAuthenticatedMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User must be authenticated");
        }

        String username = authentication.getName();
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Username not found in authentication");
        }

        // NOTE: Authentication principal is User UUID, not username (User.userName)
        // See: /openspec/changes/events-management/specs/users/spec.md - "Authentication Principal"
        // See: /openspec/changes/events-management/design.md - "Decision 7: Authentication Principal and Username Resolution"
        // Users without member records will get IllegalStateException when member lookup fails
        try {
            return UserId.fromString(username);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid user ID in authentication: " + username, e);
        }
    }

    /**
     * Map EventRegistration to RegistrationDto (without SI card number).
     *
     * @param registration event registration
     * @return RegistrationDto without SI card
     */
    private RegistrationDto toRegistrationDto(EventRegistration registration) {
        // Lookup member details from members module
        Member member = members.findById(registration.memberId())
                .orElseThrow(() -> new IllegalStateException(
                        "Member not found for registration: " + registration.memberId()));

        return new RegistrationDto(
                member.getFirstName(),
                member.getLastName(),
                registration.registeredAt()
        );
    }

    /**
     * Map EventRegistration to OwnRegistrationDto (with SI card number).
     *
     * @param registration event registration
     * @return OwnRegistrationDto with SI card
     */
    private OwnRegistrationDto toOwnRegistrationDto(EventRegistration registration) {
        // Lookup member details from members module
        Member member = members.findById(registration.memberId())
                .orElseThrow(() -> new IllegalStateException(
                        "Member not found for registration: " + registration.memberId()));

        return new OwnRegistrationDto(
                member.getFirstName(),
                member.getLastName(),
                registration.siCardNumber().value(),
                registration.registeredAt()
        );
    }
}
