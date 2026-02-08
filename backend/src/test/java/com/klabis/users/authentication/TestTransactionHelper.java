package com.klabis.users.authentication;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Utility class to simplify transaction handling in integration tests.
 *
 * <p>This helper eliminates boilerplate code for handling optional {@link TransactionTemplate}
 * in tests that may run with or without Spring context. It provides a consistent pattern
 * for executing operations within transactions when available, while gracefully falling back
 * to non-transactional execution when not.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Autowired(required = false)
 * private TransactionTemplate transactionTemplate;
 *
 * // Instead of:
 * if (transactionTemplate != null) {
 *     transactionTemplate.executeWithoutResult(tx -> {
 *         userRepository.save(user);
 *     });
 * } else {
 *     userRepository.save(user);
 * }
 *
 * // Use:
 * TestTransactionHelper.executeInTransactionOrElse(
 *     transactionTemplate,
 *     () -> userRepository.save(user),
 *     () -> userRepository.save(user)
 * );
 * }</pre>
 *
 * <h3>Design Rationale:</h3>
 * <ul>
 *   <li>Tests should remain simple and readable - the if/else pattern adds noise</li>
 *   <li>Transactional and non-transactional paths often execute the same operation</li>
 *   <li>Centralizing this pattern makes it easier to add cross-cutting test behavior</li>
 *   <li>Follows DRY principle - no need to repeat the null-check pattern</li>
 * </ul>
 *
 * @see TransactionTemplate
 */
public final class TestTransactionHelper {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private TestTransactionHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Executes an operation within a transaction if template is available, otherwise executes
     * the non-transactional alternative.
     *
     * <p>This method is useful for tests that may run with or without Spring context.
     * When TransactionTemplate is available (Spring test with full context), the operation
     * executes within a transaction. When unavailable (unit test or slice test), it executes
     * directly without transaction management.</p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * User savedUser = TestTransactionHelper.executeInTransactionOrElse(
     *     transactionTemplate,
     *     () -> userRepository.save(user),  // transactional
     *     () -> userRepository.save(user)   // non-transactional
     * );
     * }</pre>
     *
     * @param <T>                       the return type of the operation
     * @param template                  the transaction template (may be null)
     * @param transactionalOperation    the operation to execute within a transaction (if template available)
     * @param nonTransactionalOperation the operation to execute without transaction (if template unavailable)
     * @return the result of the executed operation
     */
    public static <T> T executeInTransactionOrElse(
            TransactionTemplate template,
            Supplier<T> transactionalOperation,
            Supplier<T> nonTransactionalOperation) {

        if (template != null) {
            return template.execute(status -> transactionalOperation.get());
        } else {
            return nonTransactionalOperation.get();
        }
    }

    /**
     * Executes a void operation within a transaction if template is available,
     * otherwise executes the non-transactional alternative.
     *
     * <p>This is a convenience method for operations that don't return a value.
     * It uses {@link Runnable} instead of {@link Supplier} for void operations.</p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * TestTransactionHelper.executeInTransactionOrElse(
     *     transactionTemplate,
     *     () -> userRepository.save(user),  // transactional
     *     () -> userRepository.save(user)   // non-transactional
     * );
     * }</pre>
     *
     * @param template                  the transaction template (may be null)
     * @param transactionalOperation    the operation to execute within a transaction (if template available)
     * @param nonTransactionalOperation the operation to execute without transaction (if template unavailable)
     */
    public static void executeInTransactionOrElse(
            TransactionTemplate template,
            Runnable transactionalOperation,
            Runnable nonTransactionalOperation) {

        if (template != null) {
            template.executeWithoutResult(status -> transactionalOperation.run());
        } else {
            nonTransactionalOperation.run();
        }
    }

    /**
     * Executes an operation within a transaction if template is available,
     * otherwise executes the same operation without transaction.
     *
     * <p>This is a convenience method when the transactional and non-transactional
     * operations are identical. It eliminates the need to pass the same operation twice.</p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * User savedUser = TestTransactionHelper.executeInTransactionOrElse(
     *     transactionTemplate,
     *     () -> userRepository.save(user)
     * );
     * }</pre>
     *
     * @param <T>       the return type of the operation
     * @param template  the transaction template (may be null)
     * @param operation the operation to execute (with or without transaction)
     * @return the result of the executed operation
     */
    public static <T> T executeInTransactionOrElse(
            TransactionTemplate template,
            Supplier<T> operation) {

        return executeInTransactionOrElse(template, operation, operation);
    }

    /**
     * Executes a void operation within a transaction if template is available,
     * otherwise executes the same operation without transaction.
     *
     * <p>This is a convenience method for void operations when the transactional
     * and non-transactional operations are identical.</p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * TestTransactionHelper.executeInTransactionOrElse(
     *     transactionTemplate,
     *     () -> userRepository.deleteAll()
     * );
     * }</pre>
     *
     * @param template  the transaction template (may be null)
     * @param operation the operation to execute (with or without transaction)
     */
    public static void executeInTransactionOrElse(
            TransactionTemplate template,
            Runnable operation) {

        executeInTransactionOrElse(template, operation, operation);
    }
}
