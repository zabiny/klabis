package club.klabis.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ConversionUtils {
    private ConversionUtils() {}

    public static <T> List<T> list(Collection<T> value) {
        if (value == null) {
            return List.of();
        } else {
            return new ArrayList<>(value);
        }
    }


}
