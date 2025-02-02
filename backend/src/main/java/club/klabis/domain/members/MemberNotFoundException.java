package club.klabis.domain.members;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String message) {
        super(message);
    }

    public MemberNotFoundException(Member.Id memberId) {
        this("Member with ID '%s' doesnt exist".formatted(memberId.value()));
    }
}
