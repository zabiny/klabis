package com.klabis.membershipfees.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.LocalDate;

@PrimaryPort
public interface CampaignEndProcessingPort {

    void processCampaignEnd(LocalDate toDate);

}
