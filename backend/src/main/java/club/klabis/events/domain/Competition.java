package club.klabis.events.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
        return new HashSet<>(categories);
    }

    public void setCategories(Collection<Category> categories) {
        if (
                this.getEventRegistrations().stream().map(Registration::getCategory).anyMatch(categories::contains)
        ) {
            throw new RuntimeException("Cannot change categories - there are registrations with removed category");
        }

        this.categories.clear();
        this.categories.addAll(categories);
    }


}
