package club.klabis.events.infrastructure.restapi.dto;

import org.springframework.hateoas.RepresentationModel;

public class EventRegistrationResponse extends RepresentationModel<EventRegistrationResponse> {
    private int memberId;
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }
}
