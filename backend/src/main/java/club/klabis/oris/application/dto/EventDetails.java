package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RecordBuilder
public record EventDetails(
        @JsonProperty("ID")
        int id,

        @JsonProperty("Name")
        String name,

        @JsonProperty("Date")
        LocalDate date,

        @JsonProperty("Place")
        String place,

        @JsonProperty("Map")
        String map,

        @JsonProperty("Org1")
        Organizer org1,

        @JsonProperty("Org2")
        Organizer org2,

        @JsonProperty("Region")
        String region,

        @JsonProperty("Regions")
        Map<String, Region> regions,

        @JsonProperty("Sport")
        Sport sport,

        @JsonProperty("Discipline")
        Discipline discipline,

        @JsonProperty("Level")
        Level level,

        @JsonFormat(pattern = "yyyy-MM-dd' 'HH:mm:ss", timezone = "Europe/Prague")
        @JsonProperty("EntryStart")
        ZonedDateTime entryStart,

        @JsonFormat(pattern = "yyyy-MM-dd' 'HH:mm:ss", timezone = "Europe/Prague")
        @JsonProperty("EntryDate1")
        ZonedDateTime entryDate1,

        @JsonFormat(pattern = "yyyy-MM-dd' 'HH:mm:ss", timezone = "Europe/Prague")
        @JsonProperty("EntryDate2")
        ZonedDateTime entryDate2,

        @JsonFormat(pattern = "yyyy-MM-dd' 'HH:mm:ss", timezone = "Europe/Prague")
        @JsonProperty("EntryDate3")
        ZonedDateTime entryDate3,

        @JsonProperty("EntryInfo")
        String entryInfo,

        @JsonProperty("EntryDescCZ")
        String entryDescCZ,

        @JsonProperty("EntryDescEN")
        String entryDescEN,

        @JsonProperty("EntryKoef2")
        String entryKoef2,

        @JsonProperty("EntryKoef3")
        String entryKoef3,

        @JsonProperty("EntryNoRegExtraKoef")
        String entryNoRegExtraKoef,

        @JsonProperty("EntryTotalLimit")
        String entryTotalLimit,

        @JsonProperty("EntryWaitingQueue")
        String entryWaitingQueue,

        @JsonProperty("EntryRentSIFee")
        String entryRentSIFee,

        @JsonProperty("EntryBankAccount")
        String entryBankAccount,

        @JsonProperty("UseORISForEntries")
        String useORISForEntries,

        @JsonProperty("UseWaves")
        String useWaves,

        @JsonProperty("Currency")
        String currency,

        @JsonProperty("SIType")
        SIType siType,

        @JsonProperty("WRE")
        String wre,

        @JsonProperty("LiveloxID")
        String liveloxID,

        @JsonProperty("Ranking")
        String ranking,

        @JsonProperty("RankingKoef")
        String rankingKoef,

        @JsonProperty("RankingKS")
        String rankingKS,

        @JsonProperty("RankingDecisionDate")
        String rankingDecisionDate,

        @JsonProperty("Cancelled")
        String cancelled,

        @JsonProperty("CancelledReason")
        String cancelledReason,

        @JsonProperty("StartTime")
        String startTime,

        @JsonProperty("EventOfficeCloses")
        String eventOfficeCloses,

        @JsonProperty("Protocol")
        String protocol,

        @JsonProperty("GPSLat")
        String gpsLat,

        @JsonProperty("GPSLon")
        String gpsLon,

        @JsonProperty("RequestedStartForbidden")
        String requestedStartForbidden,

        @JsonProperty("RequestedStartTextOnly")
        String requestedStartTextOnly,

        @JsonProperty("ForceAllStagesEntry")
        String forceAllStagesEntry,

        @JsonProperty("Version")
        String version,

        @JsonProperty("Stages")
        String stages,

        @JsonProperty("Stage1")
        String stage1,

        @JsonProperty("Stage2")
        String stage2,

        @JsonProperty("Stage3")
        String stage3,

        @JsonProperty("Stage4")
        String stage4,

        @JsonProperty("Stage5")
        String stage5,

        @JsonProperty("Stage6")
        String stage6,

        @JsonProperty("Stage7")
        String stage7,

        @JsonProperty("MultiEvents")
        String multiEvents,

        @JsonProperty("MultiEvent1")
        String multiEvent1,

        @JsonProperty("MultiEvent2")
        String multiEvent2,

        @JsonProperty("MultiEvent3")
        String multiEvent3,

        @JsonProperty("ParentID")
        String parentID,

        @JsonProperty("Director")
        Person director,

        @JsonProperty("MainReferee")
        Person mainReferee,

        @JsonProperty("IOFAdviser")
        List<Person> iofAdviser,

        @JsonProperty("CourseSetter_1")
        Person courseSetter1,

        @JsonProperty("SW")
        Person sw,

        @JsonProperty("EventInfo")
        String eventInfo,

        @JsonProperty("EventWarning")
        String eventWarning,

        @JsonProperty("Documents")
        Map<String, Document> documents,

        @JsonProperty("Links")
        Map<String, Link> links,

        @JsonProperty("Locations")
        Map<String, Location> locations,

        @JsonProperty("Waves")
        List<Wave> waves,

        @JsonProperty("Classes")
        Map<String, EventClass> classes,

        @JsonProperty("Services")
        Map<String, Service> services,

        @JsonProperty("CurrentEntriesCount")
        int currentEntriesCount,

        @JsonProperty("CurrentStartsCount")
        int currentStartsCount,

        @JsonProperty("CurrentResultsCount")
        int currentResultsCount,

        @JsonProperty("News")
        Map<String, News> news
) {
}
