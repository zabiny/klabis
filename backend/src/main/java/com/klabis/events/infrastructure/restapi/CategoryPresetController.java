package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.events.CategoryPresetId;
import com.klabis.events.application.CategoryPresetManagementPort;
import com.klabis.events.domain.CategoryPreset;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.core.convert.ConversionService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@PrimaryAdapter
@ExposesResourceFor(CategoryPreset.class)
class CategoryPresetController implements CategoryPresetsApi {

    private final CategoryPresetManagementPort categoryPresetManagementService;
    private final ConversionService conversionService;

    CategoryPresetController(CategoryPresetManagementPort categoryPresetManagementService, ConversionService conversionService) {
        this.categoryPresetManagementService = categoryPresetManagementService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<List<CategoryPresetDto>> listPresets() {
        List<CategoryPreset> presets = categoryPresetManagementService.listAll();

        List<CategoryPresetDto> payload = presets.stream()
                .map(preset -> conversionService.convert(preset, CategoryPresetDto.class))
                .toList();

        HalResponseContext.setDomainList(presets);
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<CategoryPresetDto> getPreset(
            @PathVariable UUID id) {

        CategoryPreset preset = categoryPresetManagementService.getPreset(new CategoryPresetId(id));

        HalResponseContext.setDomain(preset);
        return ResponseEntity.ok(conversionService.convert(preset, CategoryPresetDto.class));
    }

    @Override
    public ResponseEntity<Void> createCategoryPreset(
            CreateCategoryPresetRequest request) {

        CategoryPreset.CreateCategoryPreset command = new CategoryPreset.CreateCategoryPreset(request.name(), request.categories());
        CategoryPreset created = categoryPresetManagementService.createPreset(command);

        return ResponseEntity
                .created(linkTo(methodOn(CategoryPresetsApi.class).getPreset(created.getId().value())).toUri())
                .build();
    }

    @Override
    public ResponseEntity<Void> updateCategoryPreset(
            @PathVariable UUID id,
            UpdateCategoryPresetRequest request) {

        CategoryPreset.UpdateCategoryPreset command = new CategoryPreset.UpdateCategoryPreset(request.name(), request.categories());
        categoryPresetManagementService.updatePreset(new CategoryPresetId(id), command);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCategoryPreset(
            @PathVariable UUID id) {

        categoryPresetManagementService.deletePreset(new CategoryPresetId(id));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class CategoryPresetDetailsPostprocessor extends ModelWithDomainPostprocessor<CategoryPresetDto, CategoryPreset> {

    @Override
    public void process(EntityModel<CategoryPresetDto> dtoModel, CategoryPreset preset) {
        UUID id = preset.getId().value();
        klabisLinkTo(methodOn(CategoryPresetsApi.class).getPreset(id)).ifPresent(link ->
                dtoModel.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(CategoryPresetsApi.class).updateCategoryPreset(id, null)))
                        .andAffordances(klabisAfford(methodOn(CategoryPresetsApi.class).deleteCategoryPreset(id)))));
        klabisLinkTo(methodOn(CategoryPresetsApi.class).listPresets())
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
                .andAffordances(klabisAfford(methodOn(CategoryPresetsApi.class).createCategoryPreset(null))));
        return model;
    }
}
