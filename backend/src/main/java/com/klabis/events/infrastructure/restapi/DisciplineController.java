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
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.infrastructure.restapi.SyncApi;
import com.klabis.sync.infrastructure.restapi.SyncEntityTypeParam;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

        List<String> pageDisciplineIds = page.getContent().stream()
                .map(discipline -> discipline.getId().value().toString())
                .toList();
        Set<String> enrolledDisciplineIds = pageDisciplineIds.isEmpty()
                ? Set.of()
                : synchronizationPort.findActiveByTargets(SyncEntityType.DISCIPLINE, pageDisciplineIds).stream()
                        .map(reference -> reference.target().entityId())
                        .collect(Collectors.toSet());
        HalResponseContext.setContext(new EnrolledDisciplineIds(enrolledDisciplineIds));

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

/**
 * Carries which disciplines of the listed page are paired for synchronisation, from
 * {@code DisciplineController#listDisciplines} to {@code DisciplineDetailsPostprocessor} (one batch lookup per page).
 */
record EnrolledDisciplineIds(Set<String> disciplineIds) {

    boolean contains(UUID disciplineId) {
        return disciplineIds.contains(disciplineId.toString());
    }
}

@MvcComponent
class DisciplineDetailsPostprocessor extends ModelWithDomainPostprocessor<DisciplineDto, Discipline> {

    private final SynchronizationPort synchronizationPort;

    DisciplineDetailsPostprocessor(SynchronizationPort synchronizationPort) {
        this.synchronizationPort = synchronizationPort;
    }

    @Override
    public void process(EntityModel<DisciplineDto> dtoModel, Discipline discipline) {
        UUID id = discipline.getId().value();
        boolean orisPaired = isEnrolled(id);

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

        if (orisPaired) {
            klabisLinkTo(methodOn(SyncApi.class).getSyncState(SyncEntityTypeParam.DISCIPLINES, id.toString()))
                    .ifPresent(link -> dtoModel.add(link.withRel("sync")));
        }
    }

    // List rows share this postprocessor with the detail: the page's batch context is used when present,
    // otherwise (detail) the pairing is looked up for the single discipline.
    private boolean isEnrolled(UUID disciplineId) {
        return HalResponseContext.findContext(EnrolledDisciplineIds.class)
                .map(enrolled -> enrolled.contains(disciplineId))
                .orElseGet(() -> synchronizationPort.findByTarget(
                        new SyncTarget(SyncEntityType.DISCIPLINE, disciplineId.toString())).isPresent());
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
