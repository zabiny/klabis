package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierOptionsPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("Collection postprocessors of membershipfees only fire for their own DTO type")
class CollectionPostprocessorScopingTest {

    record OtherDto(String value) {}

    private final MembershipFeeTierManagementPort tierPort = mock(MembershipFeeTierManagementPort.class);
    private final FeeSelectionCampaignManagementPort campaignPort = mock(FeeSelectionCampaignManagementPort.class);
    private final MembershipFeeTierOptionsPort optionsPort = mock(MembershipFeeTierOptionsPort.class);
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);

    private final RepresentationModelProcessorInvoker invoker = new RepresentationModelProcessorInvoker(
            List.<RepresentationModelProcessor<?>>of(
                    new FeeSelectionCampaignListPostprocessor(tierPort),
                    new MembershipFeeTierListPostprocessor(evaluator, campaignPort),
                    new MembershipFeeTierListRulesPostprocessor(optionsPort)));

    private void assertUntouchedFor(Object element, ResolvableType elementType) {
        List<EntityModel<Object>> items = element == null
                ? List.of()
                : List.of(EntityModel.of(element));
        CollectionModel<EntityModel<Object>> model = CollectionModel.<EntityModel<Object>>of(items)
                .withFallbackType(ResolvableType.forClassWithGenerics(EntityModel.class, elementType));
        model.add(Link.of("/self", IanaLinkRelations.SELF));

        invoker.invokeProcessorsFor(model, ResolvableType.forClassWithGenerics(CollectionModel.class,
                ResolvableType.forClassWithGenerics(EntityModel.class, elementType)));

        assertThat(model.getRequiredLink(IanaLinkRelations.SELF).getAffordances()).isEmpty();
        assertThat(model.getLinks()).hasSize(1);
        verifyNoInteractions(tierPort, campaignPort, optionsPort, evaluator);
    }

    @Test
    @DisplayName("non-empty list of another DTO with Object fallback element type")
    void otherDtoObjectType() {
        assertUntouchedFor(new OtherDto("a"), ResolvableType.forClass(Object.class));
    }

    @Test
    @DisplayName("empty list with Object fallback element type")
    void emptyObjectType() {
        assertUntouchedFor(null, ResolvableType.forClass(Object.class));
    }

    @Test
    @DisplayName("non-empty list of another DTO with its declared element type")
    void otherDtoDeclaredType() {
        assertUntouchedFor(new OtherDto("a"), ResolvableType.forClass(OtherDto.class));
    }

    @Test
    @DisplayName("empty list of another DTO with its declared element type")
    void emptyOtherDtoDeclaredType() {
        assertUntouchedFor(null, ResolvableType.forClass(OtherDto.class));
    }

    @Test
    @DisplayName("control: the same setup does fire the campaign processor for its own DTO type")
    void ownDtoFires() {
        CollectionModel<EntityModel<Object>> model = CollectionModel.<EntityModel<Object>>of(List.of())
                .withFallbackType(ResolvableType.forClassWithGenerics(EntityModel.class, FeeSelectionCampaignResponse.class));
        model.add(Link.of("/self", IanaLinkRelations.SELF));

        invoker.invokeProcessorsFor(model, ResolvableType.forClassWithGenerics(CollectionModel.class,
                ResolvableType.forClassWithGenerics(EntityModel.class, FeeSelectionCampaignResponse.class)));

        org.mockito.Mockito.verify(tierPort).listTiers();
    }
}
