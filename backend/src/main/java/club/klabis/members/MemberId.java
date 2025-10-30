package club.klabis.members;

public record MemberId(int value) {

    public String toString() {
        return Integer.toString(value());
    }

}
