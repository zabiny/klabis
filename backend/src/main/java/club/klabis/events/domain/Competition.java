package club.klabis.events.domain;

import club.klabis.events.domain.forms.EventEditationForm;
import org.jmolecules.ddd.annotation.Entity;

@Entity
public class Competition extends Event {
    public Competition() {
        super();
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
}
