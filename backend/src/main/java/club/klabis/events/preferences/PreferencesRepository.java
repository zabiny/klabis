package club.klabis.events.preferences;

import club.klabis.members.MemberId;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.shared.application.DataRepository;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
@SecondaryPort
public interface PreferencesRepository extends DataRepository<Preferences, MemberId> {

    Optional<Preferences> findByRegistrationNumber(RegistrationNumber registrationNumber);

}
