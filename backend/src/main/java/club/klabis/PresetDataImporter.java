package club.klabis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;

@ConditionalOnProperty(name = "klabis.preset-data", havingValue = "true", matchIfMissing = true)
@Component
class PresetDataImporter implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PresetDataImporter.class);

    private final Collection<PresetDataLoader> loaders;

    PresetDataImporter(Collection<PresetDataLoader> loaders) {
        this.loaders = loaders;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        for (PresetDataLoader loader : loaders) {
            LOG.debug("Loading preset data using {}", loader.getClass().getSimpleName());
            loader.loadData();
        }
    }

}
