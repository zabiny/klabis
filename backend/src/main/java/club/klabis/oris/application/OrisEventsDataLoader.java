package club.klabis.oris.application;

import club.klabis.PresetDataLoader;
import club.klabis.oris.application.dto.OrisEventListFilter;

// when ORIS integration is enabled, it loads some events from ORIS when application starts.
@OrisIntegrationComponent
class OrisEventsDataLoader implements PresetDataLoader {

    private final DefaultOrisEventsImporter defaultOrisEventsImporter;

    OrisEventsDataLoader(DefaultOrisEventsImporter defaultOrisEventsImporter) {
        this.defaultOrisEventsImporter = defaultOrisEventsImporter;
    }

    @Override
    public void loadData() {
        defaultOrisEventsImporter.loadOrisEvents(OrisEventListFilter.createDefault());
    }
}
