package com.klabis.common.ui.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.common.ui.HalFormsOptionsDef;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Test fixture controller for {@code EnumOptionsAutoConfigurationTest}. Lives in a
 * {@code .infrastructure.restapi} package because {@code EnumOptionsAutoConfiguration} only scans
 * record DTOs in that package suffix — real request DTOs are generated there too.
 */
@RestController
@RequestMapping("/api/testAutoEnumOptions")
public class AutoEnumOptionsExampleController {

    @GetMapping(value = "/plain", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<AutoEnumOptionsDummy>> getPlain() {
        EntityModel<AutoEnumOptionsDummy> model = EntityModel.of(new AutoEnumOptionsDummy("dummy"));
        klabisLinkTo(methodOn(AutoEnumOptionsExampleController.class).getPlain()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(AutoEnumOptionsExampleController.class).editPlain(null)))));
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/nullable", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<AutoEnumOptionsDummy>> getNullable() {
        EntityModel<AutoEnumOptionsDummy> model = EntityModel.of(new AutoEnumOptionsDummy("dummy"));
        klabisLinkTo(methodOn(AutoEnumOptionsExampleController.class).getNullable()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(AutoEnumOptionsExampleController.class).editNullable(null)))));
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/multi", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<AutoEnumOptionsDummy>> getMulti() {
        EntityModel<AutoEnumOptionsDummy> model = EntityModel.of(new AutoEnumOptionsDummy("dummy"));
        klabisLinkTo(methodOn(AutoEnumOptionsExampleController.class).getMulti()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(AutoEnumOptionsExampleController.class).editMulti(null)))));
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/explicit", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<AutoEnumOptionsDummy>> getExplicit() {
        EntityModel<AutoEnumOptionsDummy> model = EntityModel.of(new AutoEnumOptionsDummy("dummy"));
        klabisLinkTo(methodOn(AutoEnumOptionsExampleController.class).getExplicit()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(AutoEnumOptionsExampleController.class).editPlain(null),
                                Map.of("status", HalFormsOptionsDef.values(java.util.List.of("ACTIVE")))))));
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/both", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<AutoEnumOptionsDummy>> getBoth() {
        EntityModel<AutoEnumOptionsDummy> model = EntityModel.of(new AutoEnumOptionsDummy("dummy"));
        klabisLinkTo(methodOn(AutoEnumOptionsExampleController.class).getBoth()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(AutoEnumOptionsExampleController.class).editAuto(null)))
                        .andAffordances(klabisAfford(methodOn(AutoEnumOptionsExampleController.class).editOtherAuto(null)))));
        return ResponseEntity.ok(model);
    }

    @GetMapping(value = "/choice", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    public ResponseEntity<EntityModel<AutoEnumOptionsDummy>> getChoice() {
        EntityModel<AutoEnumOptionsDummy> model = EntityModel.of(new AutoEnumOptionsDummy("dummy"));
        klabisLinkTo(methodOn(AutoEnumOptionsExampleController.class).getChoice()).ifPresent(link ->
                model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(AutoEnumOptionsExampleController.class).editChoice(null)))));
        return ResponseEntity.ok(model);
    }

    @PutMapping(value = "/plain", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editPlain(@RequestBody PlainEnumEditRequest request) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/nullable", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editNullable(@RequestBody NullableEnumEditRequest request) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/multi", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editMulti(@RequestBody MultiEnumEditRequest request) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/choice", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editChoice(@RequestBody ChoiceEditRequest request) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/auto", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editAuto(@RequestBody PlainEnumEditRequest request) {
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/otherAuto", produces = MediaTypes.HAL_JSON_VALUE)
    public ResponseEntity<Void> editOtherAuto(@RequestBody OtherStatusEditRequest request) {
        return ResponseEntity.noContent().build();
    }
}

enum AutoOptionsStatus {
    ACTIVE("ACTIVE"), INACTIVE("INACTIVE"), ARCHIVED("ARCHIVED");

    private final String value;

    AutoOptionsStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

enum AutoOptionsChoice {
    YES("YES"), NO("NO"), UNSET(null);

    private final String value;

    AutoOptionsChoice(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}

record PlainEnumEditRequest(AutoOptionsStatus status) {
}

/**
 * A distinct DTO type that happens to share the "status" property name with
 * {@link PlainEnumEditRequest} but a different enum type/values — models two different affordances
 * in one response, both auto-detected, to prove each keeps its own (type, name)-scoped options
 * rather than colliding on the shared property name.
 */
record OtherStatusEditRequest(AutoOptionsChoice status) {
}

record NullableEnumEditRequest(JsonNullable<AutoOptionsStatus> nullableStatus) {
}

record MultiEnumEditRequest(Set<AutoOptionsStatus> statuses) {
}

record ChoiceEditRequest(AutoOptionsChoice choice) {
}

record AutoEnumOptionsDummy(String value) {
}
