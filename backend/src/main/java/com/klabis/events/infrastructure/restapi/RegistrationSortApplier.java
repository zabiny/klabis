package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.EventRegistration;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies server-side in-memory sorting to the registration list.
 * <p>
 * Supported sort fields: firstName, lastName, category, registrationTime.
 * Default sort is registrationTime ASC (FCFS order).
 * <p>
 * sort=registrationTime is silently ignored when the caller is not authorized
 * (not the event coordinator and does not hold EVENTS:REGISTRATIONS).
 * Any unknown sort field also silently falls back to the default.
 */
class RegistrationSortApplier {

    private static final Set<String> ALLOWED_FIELDS = Set.of("firstName", "lastName", "category", "registrationTime");

    private RegistrationSortApplier() {
    }

    /**
     * Sorts the given registrations list according to the sort parameter.
     *
     * @param registrations               the registrations to sort
     * @param memberIndex                 member data indexed by member ID (for name-based sorting)
     * @param sort                        the raw sort parameter value (e.g. "lastName" or "lastName,desc"), may be null
     * @param callerCanSortByRegistrationTime whether the caller is authorized to use registrationTime sort
     * @return sorted copy of the registrations list
     */
    static List<EventRegistration> sort(
            List<EventRegistration> registrations,
            Map<MemberId, MemberDto> memberIndex,
            @Nullable String sort,
            boolean callerCanSortByRegistrationTime) {

        if (sort == null || sort.isBlank()) {
            return sortByRegistrationTimeAsc(registrations);
        }

        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        boolean descending = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());

        if (!ALLOWED_FIELDS.contains(field)) {
            return sortByRegistrationTimeAsc(registrations);
        }

        if ("registrationTime".equals(field) && !callerCanSortByRegistrationTime) {
            return sortByRegistrationTimeAsc(registrations);
        }

        Comparator<EventRegistration> comparator = buildComparator(field, memberIndex);
        if (descending) {
            comparator = comparator.reversed();
        }
        return registrations.stream().sorted(comparator).toList();
    }

    private static List<EventRegistration> sortByRegistrationTimeAsc(List<EventRegistration> registrations) {
        return registrations.stream()
                .sorted(Comparator.comparing(EventRegistration::registeredAt))
                .toList();
    }

    private static Comparator<EventRegistration> buildComparator(String field, Map<MemberId, MemberDto> memberIndex) {
        return switch (field) {
            case "firstName" -> Comparator.comparing(
                    r -> memberName(r, memberIndex, true),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
            case "lastName" -> Comparator.comparing(
                    r -> memberName(r, memberIndex, false),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
            case "category" -> Comparator.comparing(
                    EventRegistration::category,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
            default -> Comparator.comparing(EventRegistration::registeredAt);
        };
    }

    @Nullable
    private static String memberName(EventRegistration registration, Map<MemberId, MemberDto> memberIndex, boolean firstName) {
        MemberDto member = memberIndex.get(registration.memberId());
        if (member == null) {
            return null;
        }
        return firstName ? member.firstName() : member.lastName();
    }
}
