package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record EventClass(
        @JsonProperty("ID")
        String id,

        @JsonProperty("Name")
        String name,

        @JsonProperty("Distance")
        String distance,

        @JsonProperty("Climbing")
        String climbing,

        @JsonProperty("Controls")
        String controls,

        @JsonProperty("PreFormattedHeader")
        String preFormattedHeader,

        @JsonProperty("Splits")
        Integer splits,

        @JsonProperty("ClassDefinition")
        ClassDefinition classDefinition,

        @JsonProperty("Fee")
        String fee,

        @JsonProperty("NoExtraFee")
        String noExtraFee,

        @JsonProperty("ManualFee")
        String manualFee,

        @JsonProperty("ManualFeeEntryDate2")
        String manualFeeEntryDate2,

        @JsonProperty("ManualFeeEntryDate3")
        String manualFeeEntryDate3,

        @JsonProperty("NoRegManualFee")
        String noRegManualFee,

        @JsonProperty("NoRegManualFeeEntryDate1")
        String noRegManualFeeEntryDate1,

        @JsonProperty("NoRegManualFeeEntryDate2")
        String noRegManualFeeEntryDate2,

        @JsonProperty("NoRegManualFeeEntryDate3")
        String noRegManualFeeEntryDate3,

        @JsonProperty("Ranking")
        String ranking,

        @JsonProperty("RankingKoef")
        String rankingKoef,

        @JsonProperty("RankingKS")
        String rankingKS,

        @JsonProperty("CurrentEntriesCount")
        String currentEntriesCount,

        @JsonProperty("CurrentResultsCount")
        String currentResultsCount,

        @JsonProperty("EntryLimit")
        String entryLimit,

        @JsonProperty("Wave")
        String wave,

        @JsonProperty("OnlyRegistered")
        String onlyRegistered,

        @JsonProperty("EntryForbidden")
        String entryForbidden
) {
}
