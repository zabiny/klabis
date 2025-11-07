package club.klabis.events.domain;

import club.klabis.events.domain.forms.EventEditationForm;
import org.jmolecules.ddd.annotation.Entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Competition extends Event {
    public Competition() {
        super();
    }

    private final Set<Category> categories = new HashSet<>();

    public record Category(String name) {

    }

    public static Event newEvent(EventEditationForm form) {
        Event event = new Competition();
        event.edit(form);
        return event;
    }

    public static Event importFrom(OrisData orisData) {
        Event event = new Competition();
        event.synchronize(orisData);
        return event;
    }

    public Set<Category> getCategories() {
        return new HashSet<>(categories);
    }

    @Override
    public void synchronize(OrisData orisData) {
        super.synchronize(orisData);
        setCategories(orisData.categories().stream().map(Category::new).toList());
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
