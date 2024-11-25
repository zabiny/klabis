package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUserNotFound;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import club.klabis.domain.members.Member;

import java.util.NoSuchElementException;
import java.util.Optional;

class ApplicationUsersInMemoryRepository extends InMemoryRepositoryImpl<ApplicationUser, ApplicationUser.Id> implements ApplicationUsersRepository {
    ApplicationUsersInMemoryRepository() {
        super(ApplicationUser::getId);
    }

    @Override
    public Optional<ApplicationUser> findByUserName(String username) {
        return this.findAll().stream().filter(it -> username.equals(it.getUsername())).findAny();
    }

    @Override
    public Optional<ApplicationUser> findByGoogleSubject(String googleSub) {
        return this.findAll().stream().filter(it -> it.getGoogleSubject().filter(googleSub::equals).isPresent()).findAny();
    }

    @Override
    public ApplicationUser findByMemberId(Member.Id memberId) {
        return findAll().stream()
                .filter(it -> it.getMemberId().filter(id -> id.equals(memberId)).isPresent())
                .findAny().orElseThrow(() -> ApplicationUserNotFound.forMemberId(memberId));
    }
}
