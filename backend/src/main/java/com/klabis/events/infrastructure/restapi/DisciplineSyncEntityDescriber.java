package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.events.DisciplineId;
import com.klabis.events.application.DisciplineManagementPort;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.infrastructure.restapi.EntityDescription;
import com.klabis.sync.infrastructure.restapi.SyncEntityDescriber;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
class DisciplineSyncEntityDescriber implements SyncEntityDescriber {

    private final DisciplineManagementPort disciplineManagement;

    DisciplineSyncEntityDescriber(DisciplineManagementPort disciplineManagement) {
        this.disciplineManagement = disciplineManagement;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.DISCIPLINE;
    }

    @Override
    public Optional<EntityDescription> describe(String entityId) {
        try {
            var discipline = disciplineManagement.get(new DisciplineId(UUID.fromString(entityId)));
            // Disciplines have no detail page of their own, only the list.
            return Optional.of(EntityDescription.of(discipline.getName(),
                    klabisLinkTo(methodOn(DisciplinesApi.class).listDisciplines(null))
                            .map(WebMvcLinkBuilder::withSelfRel)));
        } catch (DisciplineNotFoundException e) {
            return Optional.empty();
        }
    }
}
