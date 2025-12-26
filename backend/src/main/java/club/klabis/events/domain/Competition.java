package club.klabis.events.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Competition extends Event {
    public Competition(String name, LocalDate eventDate) {
        super(name, eventDate);
    }

    protected Competition() {
        super();
    }

    private final Set<Category> categories = new HashSet<>();

    public record Category(String name) {
        public static Set<Category> categories(String... categoryNames) {
            return Stream.of(categoryNames).map(Category::new).collect(Collectors.toSet());
        }
    }

    public static Competition newEvent(String name, LocalDate eventDate, Set<Category> categories) {
        Competition result = new Competition(name, eventDate);
        result.setCategories(categories);
        return result;
    }

    public Set<Category> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    public Set<Registration> getRegistrationsForCategory(Category category) {
        return getEventRegistrations().stream()
                .filter(registration -> registration.getCategory().equals(category))
                .collect(Collectors.toSet());
    }

    public void setCategories(Collection<Category> newCategories) {

        Set<Category> categoriesToRemoveWithRegistrations = this.categories.stream()
                .filter(Predicate.not(newCategories::contains))   // keep removed categories
                .filter(c -> !getRegistrationsForCategory(c).isEmpty()) // keep categories with registrations
                .collect(Collectors.toSet());

        if (!categoriesToRemoveWithRegistrations.isEmpty()) {
            throw EventException.createCategoriesUpdateRejectedException(getId(), categoriesToRemoveWithRegistrations);
        }

        this.categories.clear();
        this.categories.addAll(newCategories);
    }


}
