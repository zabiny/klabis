package com.klabis.events.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.CategoryPresetId;
import com.klabis.events.application.CategoryPresetManagementPort;
import com.klabis.events.domain.CategoryPreset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/category-presets", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "CategoryPresets", description = "Category preset management API")
@PrimaryAdapter
@ExposesResourceFor(CategoryPreset.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
class CategoryPresetController {

    private final CategoryPresetManagementPort categoryPresetManagementService;

    CategoryPresetController(CategoryPresetManagementPort categoryPresetManagementService) {
        this.categoryPresetManagementService = categoryPresetManagementService;
    }

    @GetMapping
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "List all category presets",
            description = "Returns all category presets. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "200", description = "List of category presets")
    ResponseEntity<CollectionModel<EntityModel<CategoryPresetDto>>> listPresets() {
        List<CategoryPreset> presets = categoryPresetManagementService.listAll();

        List<EntityModel<CategoryPresetDto>> items = presets.stream()
                .map(preset -> {
                    CategoryPresetDto dto = CategoryPresetDtoMapper.toDto(preset);
                    EntityModel<CategoryPresetDto> model = EntityModel.of(dto);
                    klabisLinkTo(methodOn(CategoryPresetController.class).getPreset(preset.getId().value()))
                            .ifPresent(link -> model.add(link.withSelfRel()
                                    .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).updatePreset(preset.getId().value(), null)))
                                    .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).deletePreset(preset.getId().value())))));
                    return model;
                })
                .toList();

        CollectionModel<EntityModel<CategoryPresetDto>> collection = CollectionModel.of(items);
        klabisLinkTo(methodOn(CategoryPresetController.class).listPresets()).ifPresent(link ->
                collection.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).createPreset(null)))));

        return ResponseEntity.ok(collection);
    }

    @GetMapping("/{id}")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Get category preset by ID",
            description = "Returns a single category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "200", description = "Category preset found")
    ResponseEntity<EntityModel<CategoryPresetDto>> getPreset(
            @Parameter(description = "Preset UUID") @PathVariable UUID id) {

        CategoryPreset preset = categoryPresetManagementService.getPreset(new CategoryPresetId(id));
        CategoryPresetDto dto = CategoryPresetDtoMapper.toDto(preset);

        EntityModel<CategoryPresetDto> model = EntityModel.of(dto);
        klabisLinkTo(methodOn(CategoryPresetController.class).getPreset(id)).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).updatePreset(id, null)))
                        .andAffordances(klabisAfford(methodOn(CategoryPresetController.class).deletePreset(id)))));
        klabisLinkTo(methodOn(CategoryPresetController.class).listPresets())
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Create a category preset",
            description = "Creates a new category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "201", description = "Category preset created")
    ResponseEntity<Void> createPreset(
            @Parameter(description = "Preset creation data")
            @Valid @RequestBody CategoryPreset.CreateCategoryPreset command) {

        CategoryPreset created = categoryPresetManagementService.createPreset(command);

        return ResponseEntity
                .created(linkTo(methodOn(CategoryPresetController.class).getPreset(created.getId().value())).toUri())
                .build();
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Update a category preset",
            description = "Updates an existing category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "204", description = "Category preset updated")
    ResponseEntity<Void> updatePreset(
            @Parameter(description = "Preset UUID") @PathVariable UUID id,
            @Parameter(description = "Preset update data") @Valid @RequestBody CategoryPreset.UpdateCategoryPreset command) {

        categoryPresetManagementService.updatePreset(new CategoryPresetId(id), command);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Delete a category preset",
            description = "Deletes a category preset. Requires EVENTS:MANAGE authority."
    )
    @ApiResponse(responseCode = "204", description = "Category preset deleted")
    ResponseEntity<Void> deletePreset(
            @Parameter(description = "Preset UUID") @PathVariable UUID id) {

        categoryPresetManagementService.deletePreset(new CategoryPresetId(id));
        return ResponseEntity.noContent().build();
    }
}
