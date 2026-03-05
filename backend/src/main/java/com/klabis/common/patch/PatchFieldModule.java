package com.klabis.common.patch;

import com.fasterxml.jackson.databind.module.SimpleModule;

public final class PatchFieldModule extends SimpleModule {

    public PatchFieldModule() {
        super("PatchFieldModule");
        addDeserializer(PatchField.class, new PatchFieldDeserializer());
    }
}
