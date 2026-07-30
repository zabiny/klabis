package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.events.CategoryPresetId;
import com.klabis.events.application.CategoryPresetManagementPort;
import com.klabis.events.domain.CategoryPreset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "CategoryPresets", description = "Category preset management API")
@PrimaryAdapter
@ExposesResourceFor(CategoryPreset.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
class CategoryPresetController implements CategoryPresetsApi {

    private final CategoryPresetManagementPort categoryPresetManagementService;

    CategoryPresetController(CategoryPresetManagementPort categoryPresetManagementService) {
        this.categoryPresetManagementService = categoryPresetManagementService;
    }

    @Operation(
            summary = "List all category presets",
            description = "Returns all category presets. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "200", description = "List of category presets")
    @Override
    public ResponseEntity<Collection<CategoryPresetDto>> listPresets() {
        List<CategoryPreset> presets = categoryPresetManagementService.listAll();

        List<CategoryPresetDto> payload = presets.stream().map(CategoryPresetDtoMapper::toDto).toList();

        HalResponseContext.setDomainList(presets);
        return ResponseEntity.ok(payload);
    }

    @Operation(
            summary = "Get category preset by ID",
            description = "Returns a single category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "200", description = "Category preset found")
    @Override
    public ResponseEntity<CategoryPresetDto> getPreset(
            @Parameter(description = "Preset UUID") @PathVariable UUID id) {

        CategoryPreset preset = categoryPresetManagementService.getPreset(new CategoryPresetId(id));

        HalResponseContext.setDomain(preset);
        return ResponseEntity.ok(CategoryPresetDtoMapper.toDto(preset));
    }

    @Operation(
            summary = "Create a category preset",
            description = "Creates a new category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "201", description = "Category preset created")
    @Override
    public ResponseEntity<Void> createCategoryPreset(
            @Parameter(description = "Preset creation data")
            @RequestBody CreateCategoryPresetRequest request) {

        CategoryPreset.CreateCategoryPreset command = new CategoryPreset.CreateCategoryPreset(request.name(), request.categories());
        CategoryPreset created = categoryPresetManagementService.createPreset(command);

        return ResponseEntity
                .created(linkTo(methodOn(CategoryPresetController.class).getPreset(created.getId().value())).toUri())
                .build();
    }

    @Operation(
            summary = "Update a category preset",
            description = "Updates an existing category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "204", description = "Category preset updated")
    @Override
    public ResponseEntity<Void> updateCategoryPreset(
            @Parameter(description = "Preset UUID") @PathVariable UUID id,
            @Parameter(description = "Preset update data") @RequestBody UpdateCategoryPresetRequest request) {

        CategoryPreset.UpdateCategoryPreset command = new CategoryPreset.UpdateCategoryPreset(request.name(), request.categories());
        categoryPresetManagementService.updatePreset(new CategoryPresetId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete a category preset",
            description = "Deletes a category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "204", description = "Category preset deleted")
    @Override
    public ResponseEntity<Void> deleteCategoryPreset(
            @Parameter(description = "Preset UUID") @PathVariable UUID id) {

        categoryPresetManagementService.deletePreset(new CategoryPresetId(id));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class CategoryPresetDetailsPostprocessor extends ModelWithDomainPostprocessor<CategoryPresetDto, CategoryPreset> {

    @Override
    public void process(EntityModel<CategoryPresetDto> dtoModel, CategoryPreset preset) {
        UUID id = preset.getId().value();
        klabisLinkTo(methodOn(CategoryPresetController.class).getPreset(id)).ifPresent(link ->
                dtoModel.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).updateCategoryPreset(id, null)))
                        .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).deleteCategoryPreset(id)))));
        klabisLinkTo(methodOn(CategoryPresetController.class).listPresets())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

/**
 * Adds the collection-level create affordance. The self link itself is built by
 * {@code HalResponseBodyAdvice} from the current request.
 */
@MvcComponent
class CategoryPresetListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<CategoryPresetDto>>> {

    @Override
    public CollectionModel<EntityModel<CategoryPresetDto>> process(
            CollectionModel<EntityModel<CategoryPresetDto>> model) {
        model.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).createCategoryPreset(null))));
        return model;
    }
}
