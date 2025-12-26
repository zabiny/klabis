package club.klabis.events.preferences;

import club.klabis.members.MemberId;
import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Repository;

@Repository
@SecondaryAdapter
interface InMemoryPreferencesRepository extends PreferencesRepository, InMemoryRepository<Preferences, MemberId> {
}
