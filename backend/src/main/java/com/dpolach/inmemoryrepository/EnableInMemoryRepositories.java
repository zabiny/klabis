package com.dpolach.inmemoryrepository;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.support.JdbcRepositoryFactoryBean;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(InMemoryRepositoriesRegistrar.class)
public @interface EnableInMemoryRepositories {

    /**
     * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
     * {@code @EnableJdbcRepositories("org.my.pkg")} instead of
     * {@code @EnableJdbcRepositories(basePackages="org.my.pkg")}.
     */
    String[] value() default {};

    /**
     * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
     * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to String-based package names.
     */
    String[] basePackages() default {};

    /**
     * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
     * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
     * each package that serves no purpose other than being referenced by this attribute.
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
     * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
     */
    ComponentScan.Filter[] includeFilters() default {};

    /**
     * Specifies which types are not eligible for component scanning.
     */
    ComponentScan.Filter[] excludeFilters() default {};

    /**
     * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
     * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
     * for {@code PersonRepositoryImpl}.
     */
    String repositoryImplementationPostfix() default "Impl";

    /**
     * Configures the location of where to find the Spring Data named queries properties file. Will default to
     * {@code META-INF/jdbc-named-queries.properties}.
     */
    String namedQueriesLocation() default "";

    /**
     * Returns the {@link FactoryBean} class to be used for each repository instance. Defaults to
     * {@link JdbcRepositoryFactoryBean}.
     */
    Class<?> repositoryFactoryBeanClass() default InMemoryRepositoryFactoryBean.class;

    /**
     * Configure the repository base class to be used to create repository proxies for this particular configuration.
     *
     * @since 2.1
     */
    Class<?> repositoryBaseClass() default SimpleInMemoryRepository.class;

    /**
     * Configure a specific {@link BeanNameGenerator} to be used when creating the repository beans.
     * @return the {@link BeanNameGenerator} to be used or the base {@link BeanNameGenerator} interface to indicate context default.
     * @since 3.4
     */
    Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

    /**
     * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
     * repositories infrastructure.
     */
    boolean considerNestedRepositories() default false;

    /**
     * Configures the name of the {@link org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations} bean
     * definition to be used to create repositories discovered through this annotation. Defaults to
     * {@code namedParameterJdbcTemplate}.
     */
    String jdbcOperationsRef() default "";

    /**
     * Configures the name of the {@link org.springframework.data.jdbc.core.convert.DataAccessStrategy} bean definition to
     * be used to create repositories discovered through this annotation. Defaults to {@code defaultDataAccessStrategy}.
     */
    String dataAccessStrategyRef() default "";

    /**
     * Configures the name of the {@link DataSourceTransactionManager} bean definition to be used to create repositories
     * discovered through this annotation. Defaults to {@code transactionManager}.
     *
     * @since 2.1
     */
    String transactionManagerRef() default "transactionManager";

    /**
     * Returns the key of the {@link QueryLookupStrategy} to be used for lookup queries for query methods. Defaults to
     * {@link QueryLookupStrategy.Key#CREATE_IF_NOT_FOUND}.
     *
     * @since 2.1
     */
    QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;
}