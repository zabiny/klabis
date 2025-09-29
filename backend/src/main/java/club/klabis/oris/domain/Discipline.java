package club.klabis.oris.domain;

public enum Discipline {
    KL("Klasická trať"),
    KT("Krátká trať"),
    SP("Sprint"),
    DT("Dlouhá trať"),
    ST("Štafety"),
    DR("Družstva"),
    SC("Volné pořadí kontrol"),
    NOB("Noční"),
    Z("Dlouhodobé žebříčky"),
    TeO("TempO"),
    S("Školení, schůze, semináře"),
    ET("Etapový"),
    MS("Hromadný start"),
    SS("Sprintové štafety"),
    KO("Knock-out sprint"),
    STK("Stacionární tréninkový kemp"),
    D("Dráha");

    private final String nameCZ;

    Discipline(String nameCZ) {
        this.nameCZ = nameCZ;
    }
}
