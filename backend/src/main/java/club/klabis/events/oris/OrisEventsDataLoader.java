package club.klabis.events.oris;

import club.klabis.PresetDataLoader;
import club.klabis.events.oris.dto.OrisEventListFilter;
import club.klabis.shared.application.OrisIntegrationComponent;

// when ORIS integration is enabled, it loads some events from ORIS when application starts.
@OrisIntegrationComponent
class OrisEventsDataLoader implements PresetDataLoader {

    private final OrisSynchronizationUseCase synchronizationUseCase;

    OrisEventsDataLoader(OrisSynchronizationUseCase synchronizationUseCase) {
        this.synchronizationUseCase = synchronizationUseCase;
    }


    @Override
    public void loadData() {
        synchronizationUseCase.loadOrisEvents(OrisEventListFilter.createDefault());
    }
}
