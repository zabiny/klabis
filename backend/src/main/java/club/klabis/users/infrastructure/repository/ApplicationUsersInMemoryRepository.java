package club.klabis.users.infrastructure.repository;

import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.domain.ApplicationUser;
import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;

@SecondaryAdapter
interface ApplicationUsersInMemoryRepository extends ApplicationUsersRepository, InMemoryRepository<ApplicationUser, ApplicationUser.Id> {
}
