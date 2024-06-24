package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import org.springframework.stereotype.Repository;

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
    public Optional<ApplicationUser> getUserForMemberId(int memberId) {
        return findAll().stream().filter(it -> it.getMemberId().filter(id -> id == memberId).isPresent()).findFirst();
    }
}
