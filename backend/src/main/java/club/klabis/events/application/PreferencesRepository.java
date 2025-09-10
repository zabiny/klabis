package club.klabis.events.application;

import club.klabis.events.domain.Preferences;
import club.klabis.members.MemberId;
import club.klabis.shared.config.inmemorystorage.DataRepository;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

@Repository
@SecondaryPort
public interface PreferencesRepository extends DataRepository<Preferences, MemberId> {
}
