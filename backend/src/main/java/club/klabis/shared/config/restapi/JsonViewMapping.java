package club.klabis.shared.config.restapi;

public @interface JsonViewMapping {

    String name();

    Class<?> jsonView();

}
