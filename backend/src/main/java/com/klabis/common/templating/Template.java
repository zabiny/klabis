package com.klabis.common.templating;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public record Template(
        Resource template,
        Map<String, Object> variables) {

    public String getTemplateName() {
        return template.getFilename();
    }

    public String getTemplateContent() throws IOException {
        return template.getContentAsString(StandardCharsets.UTF_8);
    }

}
