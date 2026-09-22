package com.klabis.common.bootstrap;

final class SampleDataBootstrapNames {

    static final String[] ALL = {
            "MembersDataBootstrap",
            "TrainingGroupDataBootstrap",
            "EventsDataBootstrap",
            "MembershipFeeTiersDataBootstrap"
    };

    private SampleDataBootstrapNames() {
    }

    static boolean matchesAny(Class<?> type) {
        if (type == null) {
            return false;
        }
        for (String simpleName : ALL) {
            if (type.getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }
}
