package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.events.DisciplineId;
import com.klabis.events.application.DisciplineManagementPort;
import com.klabis.events.domain.Discipline;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@PrimaryAdapter
@ExposesResourceFor(Discipline.class)
public class DisciplineController implements DisciplinesApi {

    private final DisciplineManagementPort disciplineManagementService;
    private final ConversionService conversionService;
    private final SynchronizationPort synchronizationPort;

    DisciplineController(DisciplineManagementPort disciplineManagementService, ConversionService conversionService,
                          SynchronizationPort synchronizationPort) {
        this.disciplineManagementService = disciplineManagementService;
        this.conversionService = conversionService;
        this.synchronizationPort = synchronizationPort;
    }

    @Override
    public ResponseEntity<Page<DisciplineDto>> listDisciplines(
            @PageableDefault(size = 10) @ParameterObject Pageable pageable) {

        Page<Discipline> page = disciplineManagementService.list(pageable);

        HalResponseContext.setDomainList(page.getContent());
        return ResponseEntity.ok(page.map(discipline -> conversionService.convert(discipline, DisciplineDto.class)));
    }

    @Override
    public ResponseEntity<Void> createDiscipline(
            CreateDisciplineRequest request) {

        Discipline created = disciplineManagementService.create(
                conversionService.convert(request, Discipline.CreateDiscipline.class));
        return ResponseEntity
                .created(linkTo(methodOn(DisciplinesApi.class).getDiscipline(created.getId().value())).toUri())
                .build();
    }

    @Override
    public ResponseEntity<DisciplineDto> getDiscipline(
            @PathVariable UUID id) {

        DisciplineId disciplineId = new DisciplineId(id);
        Discipline discipline = disciplineManagementService.get(disciplineId);

        // Mirrors DisciplineManagementService.update's own pairing check (design.md D7/D9): the
        // postprocessor needs this to decide whether to offer updateDiscipline, but must not hold
        // SynchronizationPort itself (it is an @MvcComponent, scanned by every @WebMvcTest slice) —
        // so the controller resolves it once here and hands it over via HalResponseContext, the same
        // way EventController does for EnrolledEventIds. Task 9.4 folds this into EnrolledDisciplineIds
        // alongside the batched list-view check and the "sync" link itself.
        boolean orisPaired = synchronizationPort.findByTarget(targetFor(disciplineId)).isPresent();

        HalResponseContext.setDomain(discipline);
        HalResponseContext.setContext(new DisciplineSyncPairing(orisPaired));
        return ResponseEntity.ok(conversionService.convert(discipline, DisciplineDto.class));
    }

    @Override
    public ResponseEntity<Void> updateDiscipline(
            @PathVariable UUID id,
            UpdateDisciplineRequest request) {

        disciplineManagementService.update(new DisciplineId(id), request.code(), request.name());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> archiveDiscipline(
            @PathVariable UUID id) {

        disciplineManagementService.archive(new DisciplineId(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> restoreDiscipline(
            @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        disciplineManagementService.restore(new DisciplineId(id), actingUserId(currentUser));
        return ResponseEntity.noContent().build();
    }

    private static SyncTarget targetFor(DisciplineId id) {
        return new SyncTarget(SyncEntityType.DISCIPLINE, id.value().toString());
    }

    private static String actingUserId(CurrentUserData currentUser) {
        return currentUser.userId().uuid().toString();
    }
}

/**
 * Interim carrier (task 9.3) for the single boolean design.md D7 needs to gate {@code
 * updateDiscipline}: whether the discipline currently has any sync pairing at all, active or
 * retired. Populated once per {@code getDiscipline} request, mirroring how {@code EventController}
 * hands {@code EnrolledEventIds} to its postprocessor rather than injecting {@link
 * SynchronizationPort} into an {@code @MvcComponent}. Task 9.4 folds this into the richer {@code
 * EnrolledDisciplineIds} (batched for the list view too), which also drives the {@code sync} link.
 */
record DisciplineSyncPairing(boolean orisPaired) {
}

@MvcComponent
class DisciplineDetailsPostprocessor extends ModelWithDomainPostprocessor<DisciplineDto, Discipline> {

    @Override
    public void process(EntityModel<DisciplineDto> dtoModel, Discipline discipline) {
        UUID id = discipline.getId().value();
        boolean orisPaired = HalResponseContext.findContext(DisciplineSyncPairing.class)
                .map(DisciplineSyncPairing::orisPaired)
                .orElse(false);

        klabisLinkTo(methodOn(DisciplinesApi.class).getDiscipline(id)).ifPresent(link -> {
            var self = link.withSelfRel();
            if (!orisPaired) {
                self = self.andAffordances(klabisAfford(methodOn(DisciplinesApi.class).updateDiscipline(id, null)));
            }
            if (discipline.isArchived()) {
                self = self.andAffordances(klabisAfford(methodOn(DisciplinesApi.class).restoreDiscipline(id, null)));
            } else {
                self = self.andAffordances(klabisAfford(methodOn(DisciplinesApi.class).archiveDiscipline(id)));
            }
            dtoModel.add(self);
        });

        klabisLinkTo(methodOn(DisciplinesApi.class).listDisciplines(null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

/**
 * Collection-level affordance only — the self link itself is built by {@code HalResponseBodyAdvice}
 * from the current paging request, mirroring {@code MemberListPostprocessor}/{@code
 * EventListPostprocessor}. {@code createDiscipline}'s own {@code EVENTS_MANAGE} gate is enforced by
 * {@code klabisAfford} internally, not here.
 */
@MvcComponent
class DisciplineListPostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<DisciplineDto>>> {

    @Override
    public PagedModel<EntityModel<DisciplineDto>> process(PagedModel<EntityModel<DisciplineDto>> model) {
        model.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(DisciplinesApi.class).createDiscipline(null))));
        return model;
    }
}
