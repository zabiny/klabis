# Dotaz
How to use own implementation for @Repository beans in Spring. (= for example how to automatically create implementation of CrudRepository using own implementation which will perform all operations in memory)

---

# Vlastní implementace pro @Repository beany ve Spring

Pro vytvoření vlastní implementace rozhraní `@Repository` ve Spring frameworku (například in-memory implementace pro `CrudRepository`) máte několik možností. Níže jsou podrobně popsány hlavní přístupy.

## 1. Vytvoření vlastní továrny repozitářů (Repository Factory)

Nejčistší způsob je vytvoření vlastní továrny repozitářů, která bude generovat implementace vašich repozitářů.

### Krok 1: Vytvořte vlastní implementaci RepositoryFactorySupport

```java
public class InMemoryRepositoryFactory extends RepositoryFactorySupport {

    private final Map<Class<?>, Map<Object, Object>> inMemoryDb = new ConcurrentHashMap<>();

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleInMemoryRepository.class;
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        Class<?> domainClass = information.getDomainType();
        Class<?> idClass = information.getIdType();
        
        // Inicializace in-memory "databáze" pro danou entitu, pokud ještě neexistuje
        inMemoryDb.computeIfAbsent(domainClass, k -> new ConcurrentHashMap<>());
        
        return new SimpleInMemoryRepository<>(domainClass, inMemoryDb.get(domainClass));
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(
            QueryLookupStrategy.Key key, 
            EvaluationContextProvider evaluationContextProvider) {
        return Optional.of(new InMemoryQueryLookupStrategy());
    }
}
```


### Krok 2: Vytvořte vlastní implementaci repozitáře

```java
public class SimpleInMemoryRepository<T, ID> implements CrudRepository<T, ID> {

    private final Class<T> domainClass;
    private final Map<Object, Object> entities;
    
    // Používám reflexi pro získání a nastavení ID
    private final Method getIdMethod;

    public SimpleInMemoryRepository(Class<T> domainClass, Map<Object, Object> entities) {
        this.domainClass = domainClass;
        this.entities = entities;
        
        try {
            // Předpokládá použití @Id anotace
            this.getIdMethod = Arrays.stream(domainClass.getMethods())
                .filter(method -> method.isAnnotationPresent(Id.class) || 
                                  method.getName().equals("getId"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Nelze najít metodu pro získání ID v entitě " + domainClass.getName()));
        } catch (Exception e) {
            throw new RuntimeException("Chyba při inicializaci in-memory repository", e);
        }
    }

    @Override
    public <S extends T> S save(S entity) {
        try {
            ID id = (ID) getIdMethod.invoke(entity);
            
            // Pro nové entity vygenerujte ID, pokud je to potřeba
            if (id == null) {
                // Implementace generování ID
                id = generateId();
                // Nastavení ID do entity pomocí reflexe
                setId(entity, id);
            }
            
            entities.put(id, entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Chyba při ukládání entity", e);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable((T) entities.get(id));
    }

    @Override
    public boolean existsById(ID id) {
        return entities.containsKey(id);
    }

    @Override
    public Iterable<T> findAll() {
        return (Iterable<T>) entities.values();
    }

    @Override
    public long count() {
        return entities.size();
    }

    @Override
    public void deleteById(ID id) {
        entities.remove(id);
    }

    @Override
    public void delete(T entity) {
        try {
            ID id = (ID) getIdMethod.invoke(entity);
            entities.remove(id);
        } catch (Exception e) {
            throw new RuntimeException("Chyba při mazání entity", e);
        }
    }

    // Implementace dalších metod z CrudRepository...
    
    private ID generateId() {
        // Implementace generování ID podle typu
    }
    
    private <S extends T> void setId(S entity, ID id) {
        // Implementace nastavení ID do entity pomocí reflexe
    }
}
```


### Krok 3: Vytvořte QueryLookupStrategy pro podporu vlastních dotazů

```java
public class InMemoryQueryLookupStrategy implements QueryLookupStrategy {

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, 
                                       ProjectionFactory factory, NamedQueries namedQueries) {
        return new InMemoryRepositoryQuery(method, metadata);
    }
}

public class InMemoryRepositoryQuery implements RepositoryQuery {
    private final Method method;
    private final RepositoryMetadata metadata;
    
    public InMemoryRepositoryQuery(Method method, RepositoryMetadata metadata) {
        this.method = method;
        this.metadata = metadata;
    }
    
    @Override
    public Object execute(Object[] parameters) {
        // Implementace provádění dotazů nad in-memory kolekcí
        // Tato část by zahrnovala analýzu názvu metody a implementaci odpovídající logiky
    }
    
    // další required metody...
}
```


### Krok 4: Konfigurace a registrace vlastní továrny repozitářů

```java
@Configuration
public class InMemoryRepositoryConfig {

    @Bean
    public InMemoryRepositoryFactoryBean repositoryFactoryBean() {
        return new InMemoryRepositoryFactoryBean();
    }
    
    public static class InMemoryRepositoryFactoryBean extends RepositoryFactoryBeanSupport<Object, Object, Object> {
        
        public InMemoryRepositoryFactoryBean() {
            super(null);
        }
        
        @Override
        protected RepositoryFactorySupport createRepositoryFactory() {
            return new InMemoryRepositoryFactory();
        }
    }
}
```


## 2. Použití vlastní implementace pomocí @EnableJpaRepositories

Pokud nechcete implementovat celou infrastrukturu repozitářů, můžete použít existující Spring Data mecanismus s vlastní implementací:

```java
@Configuration
@EnableJpaRepositories(
    basePackages = "club.klabis.domain",
    repositoryBaseClass = BaseInMemoryRepository.class
)
public class RepositoryConfig {
    // Konfigurace
}
```


A následně vytvořte základní implementaci:

```java
public class BaseInMemoryRepository<T, ID> implements JpaRepository<T, ID> {
    // Implementace všech metod z JpaRepository...
}
```


## 3. Komplexní řešení: Vytvoření vlastního modulu podobného Spring Data

Pro plnohodnotné řešení můžete vytvořit vlastní modul podobný Spring Data modulům jako je Spring Data JPA nebo Spring Data MongoDB.

### Krok 1: Vytvořte anotaci pro povolení vašich repozitářů

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(InMemoryRepositoriesRegistrar.class)
public @interface EnableInMemoryRepositories {
    
    String[] basePackages() default {};
    
    Class<?>[] basePackageClasses() default {};
    
    // Další konfigurační parametry
}
```


### Krok 2: Vytvořte registrátor, který bude skenovat a registrovat repozitáře

```java
public class InMemoryRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableInMemoryRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new InMemoryRepositoryConfigurationExtension();
    }
}

public class InMemoryRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    @Override
    public String getModuleName() {
        return "InMemory";
    }

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return InMemoryRepositoryFactoryBean.class.getName();
    }
    
    // Další požadované metody
}
```


## 4. Jednoduché řešení: Manuální implementace

Pro jednodušší scénáře můžete implementovat vlastní repozitář ručně:

```java
@Repository
public class InMemoryEventRepository implements EventsRepository {
    
    private final Map<Event.Id, Event> events = new ConcurrentHashMap<>();

    @Override
    public <S extends Event> S save(S entity) {
        events.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends Event> Iterable<S> saveAll(Iterable<S> entities) {
        entities.forEach(entity -> events.put(entity.getId(), entity));
        return entities;
    }

    @Override
    public Optional<Event> findById(Event.Id id) {
        return Optional.ofNullable(events.get(id));
    }
    
    @Override
    public Optional<Event> findByOrisId(int orisEventId) {
        return events.values().stream()
                .filter(event -> event.getOrisId() == orisEventId)
            .findFirst();
    }

    // Implementace dalších metod z EventsRepository
}
```


## 5. Použití existujících implementací

Spring Data již obsahuje implementaci pro in-memory repozitáře:

```java
@Configuration
@EnableRepositories(
    basePackages = "club.klabis.domain",
    repositoryFactoryBeanClass = InMemoryRepositoryFactoryBean.class
)
public class RepositoryConfig {
    // Pokud použijete vlastní implementaci InMemoryRepositoryFactoryBean
}
```


## Praktický příklad kompletní implementace

Pro úplnost, zde je praktický příklad implementace:

```java
@Configuration
@ComponentScan("club.klabis.domain")
public class InMemoryRepositoryConfig {

    @Bean
    public RepositoryFactorySupport inMemoryRepositoryFactory() {
        return new InMemoryRepositoryFactory();
    }
    
    @Bean
    public BeanDefinitionRegistryPostProcessor repositoryProcessor() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
                // Skenování repozitářů a registrace bean definic
                ClassPathScanningCandidateComponentProvider scanner = 
                    new ClassPathScanningCandidateComponentProvider(false);
                
                scanner.addIncludeFilter(new AnnotationTypeFilter(Repository.class));
                
                for (BeanDefinition bd : scanner.findCandidateComponents("club.klabis.domain")) {
                    try {
                        Class<?> repoClass = Class.forName(bd.getBeanClassName());
                        if (repoClass.isInterface()) {
                            // Registrace proxy factory pro každé rozhraní
                            RootBeanDefinition beanDef = new RootBeanDefinition(
                                ProxyFactoryBean.class);
                            
                            beanDef.getPropertyValues().add("targetSource", 
                                new LazyInitTargetSource() {
                                    @Override
                                    public Class<?> getTargetClass() {
                                        return repoClass;
                                    }
                                    
                                    @Override
                                    public Object getTarget() {
                                        // Vytvoření proxy implementace
                                        return createRepository(repoClass);
                                    }
                                });
                            
                            String beanName = repoClass.getSimpleName();
                            registry.registerBeanDefinition(
                                Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1),
                                beanDef);
                        }
                    } catch (ClassNotFoundException e) {
                        // Logování
                    }
                }
            }
            
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) 
                throws BeansException {
                // Nic nedělá
            }
            
            private Object createRepository(Class<?> repoInterface) {
                // Použití továrny k vytvoření proxy
                return inMemoryRepositoryFactory().getRepository(repoInterface);
            }
        };
    }
}
```


S tímto nastavením můžete používat vaše repozitáře s vlastní in-memory implementací a stále zachovat Spring Data přístup a integraci.