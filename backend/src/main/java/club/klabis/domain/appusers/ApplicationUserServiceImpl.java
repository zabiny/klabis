package club.klabis.domain.appusers;

import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@org.springframework.stereotype.Service
class ApplicationUserServiceImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationUserServiceImpl.class);

    private final ApplicationUsersRepository repository;

    public ApplicationUserServiceImpl(ApplicationUsersRepository repository) {
        this.repository = repository;

        LOG.info("Adding user dpolach");
        ApplicationUser admin = ApplicationUser.newAppUser("dpolach", "{noop}secret");
        admin.linkWithGoogle("110875617296914468258");
        repository.save(admin);

        LOG.info("Adding user admin");
        admin = ApplicationUser.newAppUser("admin", "{noop}secret");
        repository.save(admin);

    }
}
