package com.klabis.common.templating;

import com.klabis.common.email.EmailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Renders email templates using Thymeleaf template engine.
 *
 * <p>Supports both HTML and plain-text template rendering.
 * Template files should be located in src/main/resources/templates/email/
 */
public class ThymeleafTemplateRenderer implements TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(ThymeleafTemplateRenderer.class);

    private final TemplateEngine templateEngine;

    public ThymeleafTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String render(Template template) {
        try {
            Context context = new Context();
            context.setVariables(template.variables());

            String html = templateEngine.process(template.getTemplateContent(), context);
            log.debug("Rendered template: {}", template.getTemplateName());
            return html;
        } catch (Exception e) {
            log.error("Failed to render template '{}': {}", template.getTemplateName(), e.getMessage());
            throw new TemplateRenderException("Failed to render template: " + template.getTemplateName(), e);
        }
    }

    /**
     * Renders an HTML template with the provided variables.
     *
     * @param template  the email template to render
     * @param variables template variables (e.g., firstName, activationUrl)
     * @return rendered HTML content
     * @throws TemplateRenderException if template rendering fails
     */
    public String renderHtml(EmailTemplate template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String html = templateEngine.process(template.getHtmlTemplatePath(), context);
            log.debug("Rendered HTML template: {}", template.getTemplateName());
            return html;
        } catch (Exception e) {
            log.error("Failed to render HTML template '{}': {}", template.getTemplateName(), e.getMessage());
            throw new TemplateRenderException("Failed to render HTML template: " + template.getTemplateName(), e);
        }
    }

    /**
     * Renders a plain-text template with the provided variables.
     *
     * @param template  the email template to render
     * @param variables template variables (e.g., firstName, activationUrl)
     * @return rendered plain-text content
     * @throws TemplateRenderException if template rendering fails
     */
    public String renderText(EmailTemplate template, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String text = templateEngine.process(template.getTextTemplatePath(), context);
            log.debug("Rendered text template: {}", template.getTemplateName());
            return text;
        } catch (Exception e) {
            log.error("Failed to render text template '{}': {}", template.getTemplateName(), e.getMessage());
            throw new TemplateRenderException("Failed to render text template: " + template.getTemplateName(), e);
        }
    }

    /**
     * Exception thrown when template rendering fails.
     */
    public static class TemplateRenderException extends RuntimeException {
        public TemplateRenderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
