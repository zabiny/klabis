package com.klabis.members;

/**
 * Enum representing driving license groups according to EU classification.
 * <p>
 * These categories define different types of motor vehicles that can be driven.
 * The categories follow the European Union driving license classification system.
 */
public enum DrivingLicenseGroup {

    /**
     * Category B - Passenger cars up to 3.5 tonnes and 8 seats (excluding driver)
     */
    B,

    /**
     * Category BE - Passenger cars with trailer
     */
    BE,

    /**
     * Category C - Trucks over 3.5 tonnes
     */
    C,

    /**
     * Category C1 - Trucks between 3.5 and 7.5 tonnes
     */
    C1,

    /**
     * Category D - Buses with more than 8 seats
     */
    D,

    /**
     * Category D1 - Buses with up to 16 seats
     */
    D1,

    /**
     * Category T - Tractors and agricultural vehicles
     */
    T,

    /**
     * Category AM - Mopeds and light quadricycles
     */
    AM,

    /**
     * Category A1 - Light motorcycles (up to 125cc, 11kW)
     */
    A1,

    /**
     * Category A2 - Medium motorcycles (up to 35kW)
     */
    A2,

    /**
     * Category A - All motorcycles
     */
    A
}
