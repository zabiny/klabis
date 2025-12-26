package club.klabis.events.domain;

import org.springframework.util.Assert;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public record OrisEventId(int value) {
    public OrisEventId(int value) {
        Assert.isTrue(value > 0, "Invalid orisId: %d (must be higher than 0)".formatted(value));
        this.value = value;
    }

    public URL createEventUrl() {
        String ORIS_EVENT_URL_STRING = "https://oris.orientacnisporty.cz/Zavod?id=%d".formatted(value());
        try {
            return URI.create(ORIS_EVENT_URL_STRING).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid oris event URL %s".formatted(ORIS_EVENT_URL_STRING), e);
        }
    }

}
