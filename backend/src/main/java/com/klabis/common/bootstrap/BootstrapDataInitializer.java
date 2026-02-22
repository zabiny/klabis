package com.klabis.common.bootstrap;

public interface BootstrapDataInitializer {
    /**
     * @return true of data initializer requires bootstrap data load
     */
    boolean requiresBootstrap();

    /**
     * Bootstraps it's data
     */
    void bootstrapData();

}