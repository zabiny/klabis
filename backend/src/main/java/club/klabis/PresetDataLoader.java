package club.klabis;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PresetDataLoader implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PresetDataLoader.class);

    private final ApplicationUsersRepository appUsersRepository;

    public PresetDataLoader(ApplicationUsersRepository appUsersRepository) {
        this.appUsersRepository = appUsersRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // create Admin users
        appUsersRepository.findByUserName("dpolach").orElseGet(() -> {
            LOG.info("Adding user dpolach");
            ApplicationUser admin = ApplicationUser.newAppUser("dpolach", "{noop}secret");
            admin.linkWithGoogle("110875617296914468258");
            return appUsersRepository.save(admin);
        });

        appUsersRepository.findByUserName("admin").orElseGet(() -> {
            LOG.info("Adding user admin");
            ApplicationUser admin = ApplicationUser.newAppUser("admin", "{noop}secret");
            return appUsersRepository.save(admin);
        });

        // ... some additional data?
    }
}
