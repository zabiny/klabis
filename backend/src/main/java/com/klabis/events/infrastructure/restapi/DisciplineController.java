package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.events.DisciplineId;
import com.klabis.events.application.DisciplineManagementPort;
import com.klabis.events.domain.Discipline;
import com.klabis.common.users.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncPairing;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.infrastructure.restapi.SyncLinks;
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

import java.util.List;
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

    DisciplineController(DisciplineManagementPort disciplineManagementService, ConversionService conversionService) {
        this.disciplineManagementService = disciplineManagementService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Page<DisciplineDto>> listDisciplines(
            @PageableDefault(size = 10) @ParameterObject Pageable pageable) {

        Page<Discipline> page = disciplineManagementService.list(pageable);

        HalResponseContext.setDomainList(page.getContent());
        return ResponseEntity.ok(page.map(discipline -> conversionService.convert(discipline, DisciplineDto.class)));
    }

    @Override
    public ResponseEntity<List<DisciplineOptionResponse>> listDisciplineOptions() {
        List<DisciplineOptionResponse> options = disciplineManagementService.listActiveOptions().stream()
                .map(discipline -> DisciplineOptionResponseBuilder.builder()
                        .value(discipline.getId().value().toString())
                        .prompt(discipline.getName())
                        .build())
                .toList();
        return ResponseEntity.ok(options);
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

        HalResponseContext.setDomain(discipline);
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

    private static String actingUserId(CurrentUserData currentUser) {
        return currentUser.userId().uuid().toString();
    }
}

/**
 * Adds the {@code disciplines} link to root navigation.
 * Authorization gated via klabisLinkTo (EVENTS_MANAGE) in the actual endpoint.
 */
@MvcComponent
class DisciplinesRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(DisciplinesApi.class).listDisciplines(null))
                .ifPresent(link -> model.add(link.withRel("disciplines")));
        return model;
    }
}

@MvcComponent
class DisciplineDetailsPostprocessor extends ModelWithDomainPostprocessor<DisciplineDto, Discipline> {

    private final SyncPairingLookup pairingLookup;
    private final SyncLinks syncLinks;

    DisciplineDetailsPostprocessor(SyncPairingLookup pairingLookup, SyncLinks syncLinks) {
        this.pairingLookup = pairingLookup;
        this.syncLinks = syncLinks;
    }

    @Override
    public void process(EntityModel<DisciplineDto> dtoModel, Discipline discipline) {
        UUID id = discipline.getId().value();
        SyncTarget syncTarget = new SyncTarget(SyncEntityType.DISCIPLINE, id.toString());
        boolean locked = pairingLookup.pairingOf(syncTarget) != SyncPairing.NONE;

        klabisLinkTo(methodOn(DisciplinesApi.class).getDiscipline(id)).ifPresent(link -> {
            var self = link.withSelfRel();
            if (!locked) {
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

        syncLinks.addTo(dtoModel, syncTarget);
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
