/**
 * Common shared infrastructure.
 *
 * <p>This package contains shared utilities and infrastructure used across all bounded contexts:
 * <ul>
 *   <li>Email services and templates</li>
 *   <li>Audit logging annotations and aspects</li>
 *   <li>Exception handling</li>
 *   <li>Common utilities</li>
 * </ul>
 *
 * <p><b>NOTE:</b> This is NOT a Spring Modulith module. It's a regular package that contains
 * shared infrastructure code. Since Spring Modulith uses {@code detection-strategy: explicitly-annotated},
 * only packages with {@code @ApplicationModule} are detected as modules.
 *
 * <p>This approach allows common types (EmailService, Auditable, etc.) to be used across
 * all modules without "non-exposed type" violations.
 */
package com.klabis.common;

