package club.klabis.shared.config.restapi;

public enum ResponseViews {

    SUMMARY(Summary.class, "summary"), DETAILED(Detailed.class, "detailed");

    final Class<?> jsonView;
    final String requestParamValue;

    ResponseViews(Class<?> jsonView, String requestParamValue) {
        this.jsonView = jsonView;
        this.requestParamValue = requestParamValue;
    }

    public Class<?> getJsonView() {
        return jsonView;
    }

    public interface Summary {
    }

    public interface Detailed extends Summary {
    }
}
