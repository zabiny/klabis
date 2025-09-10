package club.klabis.events.infrastructure.inmemoryrepo;

import club.klabis.events.application.PreferencesRepository;
import club.klabis.events.domain.Preferences;
import club.klabis.members.MemberId;
import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Repository;

@Repository
@SecondaryAdapter
interface InMemoryPreferencesRepository extends PreferencesRepository, InMemoryRepository<Preferences, MemberId> {
}
