package club.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Contact(
        Type type,
        String value,
        String note) {

    public static Contact email(String email, String note) {
        return new Contact(Type.EMAIL, email, note);
    }

    public static Contact phone(String phone, String note) {
        return new Contact(Type.PHONE, phone, note);
    }

    public enum Type {
        EMAIL, PHONE
    }
}

