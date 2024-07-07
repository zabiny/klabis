package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

class ApplicationUsersInMemoryRepository extends InMemoryRepositoryImpl<ApplicationUser, Integer> implements ApplicationUsersRepository {
    ApplicationUsersInMemoryRepository() {
        super(ApplicationUser::getId);
    }

    @Override
    public Optional<ApplicationUser> findByUserName(String username) {
        return this.findAll().stream().filter(it -> username.equals(it.getUsername())).findAny();
    }

    @Override
    public Optional<ApplicationUser> findByGoogleSubject(String googleSub) {
        return this.findAll().stream().filter(it -> googleSub.equals(it.getGoogleSubject())).findAny();
    }

    @Override
    public ApplicationUser findByMemberId(int memberId) {
        return findAll().stream().filter(it -> it.getMemberId().filter(id -> id == memberId).isPresent()).findAny().orElseThrow(() -> new NoSuchElementException("Application user for member ID %s was not found".formatted(memberId)));
    }
}
