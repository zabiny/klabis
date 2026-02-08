package com.klabis.common.email;

/**
 * Enumeration of available email templates.
 * Each template has both HTML and plain-text versions.
 */
public enum EmailTemplate {

    /**
     * Welcome email sent to new members with account activation link.
     * Template files: welcome.html, welcome.txt
     * <p>
     * Required variables:
     * - firstName: Member's first name
     * - lastName: Member's last name
     * - registrationNumber: Club registration number
     * - activationUrl: Full URL for account activation
     * - clubName: Name of the club
     */
    WELCOME("welcome"),

    /**
     * Password setup email sent to new users with password setup link.
     * Template files: password-setup.html, password-setup.txt
     * <p>
     * Required variables:
     * - firstName: User's first name
     * - setupUrl: Full URL for password setup
     * - expirationHours: Token validity period in hours
     * - clubName: Name of the club
     */
    PASSWORD_SETUP("password-setup");

    private final String templateName;

    EmailTemplate(String templateName) {
        this.templateName = templateName;
    }

    /**
     * Get the template name (without file extension).
     *
     * @return template name for use with template engine
     */
    public String getTemplateName() {
        return templateName;
    }

    /**
     * Get the path to the HTML template.
     *
     * @return template path relative to templates directory
     */
    public String getHtmlTemplatePath() {
        return "email/" + templateName + ".html";
    }

    /**
     * Get the path to the plain-text template.
     *
     * @return template path relative to templates directory
     */
    public String getTextTemplatePath() {
        return "email/" + templateName + ".txt";
    }
}
