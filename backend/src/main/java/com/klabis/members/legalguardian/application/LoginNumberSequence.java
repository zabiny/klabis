package com.klabis.members.legalguardian.application;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface LoginNumberSequence {

    /**
     * Next login number of a non-member legal guardian: EXT0001, EXT0002, ... Numbers are never reused.
     */
    String next();
}
