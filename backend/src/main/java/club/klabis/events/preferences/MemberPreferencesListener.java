package club.klabis.events.preferences;

import club.klabis.members.domain.Member;
import club.klabis.members.domain.events.MemberCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MemberPreferencesListener {

    private final PreferencesRepository preferencesRepository;

    public MemberPreferencesListener(PreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
    }

    @EventListener
    void onMemberCreated(MemberCreatedEvent event) {
        Member member = event.getAggregate();
        Preferences preferences = new Preferences(member.getId());
        preferences.setRegistrationNumber(member.getRegistration());
        preferences.setSiCardNumber(member.getSiCard().orElse(null));

        preferencesRepository.save(preferences);
    }

}
