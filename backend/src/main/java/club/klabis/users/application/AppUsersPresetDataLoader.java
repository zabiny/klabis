package club.klabis.users.application;

import club.klabis.PresetDataLoader;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.users.domain.ApplicationUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class AppUsersPresetDataLoader implements PresetDataLoader {

    private static final Logger LOG = LoggerFactory.getLogger(AppUsersPresetDataLoader.class);

    private final ApplicationUsersRepository appUsersRepository;

    public AppUsersPresetDataLoader(ApplicationUsersRepository appUsersRepository) {
        this.appUsersRepository = appUsersRepository;
    }

    @Override
    public void loadData() {
        // create Admin user
        appUsersRepository.findByUserName("admin").orElseGet(() -> {
            LOG.info("Adding user admin");
            ApplicationUser admin = ApplicationUser.newAppUser(ApplicationUser.UserName.of("admin"), "{noop}secret");
            admin.setGlobalGrants(EnumSet.allOf(ApplicationGrant.class));
            return appUsersRepository.save(admin);
        });
    }

}
