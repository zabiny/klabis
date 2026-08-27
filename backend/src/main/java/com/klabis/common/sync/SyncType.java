package com.klabis.common.sync;

// TODO: najit zpusob jak v `common` nevyjmenovavat vsechny typy objektu ktere se synchronizuji (abstract syncId bez typu? A co jeho ukladani do DB? ) nebo obratit zavislost sync (= sync bude nad moduly jejichc data synchronizuje - coz se mi asi libi o trochu vice - protoze pak bude moct takovy sync reagovat na eventy z danych modulu)
public enum SyncType {
    EVENT, MEMBER
}
