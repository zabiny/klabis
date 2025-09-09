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

    public String getRequestParamValue() {
        return requestParamValue;
    }

    public static ResponseViews ofRequestParam(String value) {
        return switch (value) {
            case "detailed":
                yield ResponseViews.DETAILED;
            case "summary":
                yield ResponseViews.SUMMARY;
            default:
                yield ResponseViews.SUMMARY;
        };
    }

    public interface Summary {
    }

    public interface Detailed extends Summary {
    }
}
