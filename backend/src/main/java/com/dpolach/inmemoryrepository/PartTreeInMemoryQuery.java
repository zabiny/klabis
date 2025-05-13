package com.dpolach.inmemoryrepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PartTreeInMemoryQuery implements RepositoryQuery {

    private final InMemoryQueryMethod method;
    private final InMemoryEntityStore entityStore;
    private final PartTree tree;
    private final Class<Object> domainClass;

    public PartTreeInMemoryQuery(InMemoryQueryMethod method, InMemoryEntityStore entityStore) {
        this.method = method;
        this.entityStore = entityStore;
        this.domainClass = (Class<Object>) method.getEntityInformation().getJavaType();
        this.tree = new PartTree(method.getName(), domainClass);
    }

    @Override
    public Object execute(Object[] parameters) {
        ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), parameters);
        Predicate<Object> predicate = buildPredicate(tree, accessor);

        // Použijeme předem připravený predikát k filtrování entit
        if (method.isCollectionQuery()) {
            List<?> result = entityStore.findAll(domainClass, predicate);

            // Aplikujeme řazení, pokud je specifikováno
            if (tree.getSort() != null && !tree.getSort().isUnsorted()) {
                // Implementace řazení by byla v reálném případě složitější
                // Pro zjednodušení ji zde neimplementujeme
            }

            // Implementace stránkování, pokud je potřeba
            if (method.getParameters().hasPageableParameter()) {
                Pageable pageable = accessor.getPageable();
                // Implementace stránkování by byla v reálném případě složitější
                // Pro zjednodušení ji zde neimplementujeme
            }

            return result;
        } else if (tree.isExistsProjection()) {
            return entityStore.findOne(domainClass, predicate).isPresent();
        } else  {
            return entityStore.findOne(domainClass, predicate);
        }
    }

    @Override
    public InMemoryQueryMethod getQueryMethod() {
        return method;
    }

    @SuppressWarnings("unchecked")
    private Predicate<Object> buildPredicate(PartTree tree, ParameterAccessor accessor) {
        List<Predicate<Object>> orPredicates = new ArrayList<>();

        Iterator<Object> parameters = accessor.iterator();

        for (PartTree.OrPart orPart : tree) {
            List<Predicate<Object>> andPredicates = new ArrayList<>();

            for (Part part : orPart) {
                andPredicates.add(buildPredicate(part, parameters.hasNext() ? parameters.next() : null));
            }

            // Spojení AND predikátů v rámci jedné OR části
            orPredicates.add(entity -> {
                for (Predicate<Object> predicate : andPredicates) {
                    if (!predicate.test(entity)) {
                        return false;
                    }
                }
                return true;
            });
        }

        // Spojení OR predikátů
        return entity -> {
            for (Predicate<Object> predicate : orPredicates) {
                if (predicate.test(entity)) {
                    return true;
                }
            }
            return false;
        };
    }

    private Predicate<Object> buildPredicate(Part part, Object value) {
        String property = part.getProperty().toDotPath();
        Part.Type type = part.getType();

        return entity -> {
            Object propertyValue = getPropertyValue(entity, property);

            switch (type) {
                case SIMPLE_PROPERTY:
                    return equals(propertyValue, value);
                case NEGATING_SIMPLE_PROPERTY:
                    return !equals(propertyValue, value);
                case GREATER_THAN:
                    return compareValues(propertyValue, value) > 0;
                case GREATER_THAN_EQUAL:
                    return compareValues(propertyValue, value) >= 0;
                case LESS_THAN:
                    return compareValues(propertyValue, value) < 0;
                case LESS_THAN_EQUAL:
                    return compareValues(propertyValue, value) <= 0;
                case LIKE:
                    return like(propertyValue, value, true, true);
                case STARTING_WITH:
                    return like(propertyValue, value, false, true);
                case ENDING_WITH:
                    return like(propertyValue, value, true, false);
                case CONTAINING:
                    return like(propertyValue, value, true, true);
                case NOT_CONTAINING:
                    return !like(propertyValue, value, true, true);
                case IS_NULL:
                    return propertyValue == null;
                case IS_NOT_NULL:
                    return propertyValue != null;
                case IN:
                    return in(propertyValue, value);
                case NOT_IN:
                    return !in(propertyValue, value);
                case TRUE:
                    return Boolean.TRUE.equals(propertyValue);
                case FALSE:
                    return Boolean.FALSE.equals(propertyValue);
                case BETWEEN:
                    if (value instanceof Object[] && ((Object[]) value).length >= 2) {
                        Object[] range = (Object[]) value;
                        return compareValues(propertyValue, range[0]) >= 0 &&
                               compareValues(propertyValue, range[1]) <= 0;
                    }
                    return false;
                default:
                    throw new UnsupportedOperationException("Unsupported operator: " + type);
            }
        };
    }

    private Object getPropertyValue(Object object, String propertyPath) {
        String[] pathParts = propertyPath.split("\\.");
        Object current = object;

        for (String part : pathParts) {
            if (current == null) {
                return null;
            }

            Field field = ReflectionUtils.findField(current.getClass(), part);
            if (field == null) {
                return null;
            }

            ReflectionUtils.makeAccessible(field);
            current = ReflectionUtils.getField(field, current);
        }

        return current;
    }

    private boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }

        if (a instanceof Comparable && b instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }

        return a.toString().compareTo(b.toString());
    }

    private boolean like(Object propertyValue, Object pattern, boolean prefix, boolean suffix) {
        if (propertyValue == null || pattern == null) {
            return false;
        }

        String propertyString = propertyValue.toString();
        String patternString = pattern.toString();

        String regex = Pattern.quote(patternString);
        if (prefix) {
            regex = ".*" + regex;
        }
        if (suffix) {
            regex = regex + ".*";
        }

        return propertyString.matches(regex);
    }

    private boolean in(Object propertyValue, Object collection) {
        if (propertyValue == null || collection == null) {
            return false;
        }

        if (collection.getClass().isArray()) {
            Object[] array = (Object[]) collection;
            for (Object item : array) {
                if (equals(propertyValue, item)) {
                    return true;
                }
            }
            return false;
        } else if (collection instanceof Collection) {
            return ((Collection<?>) collection).contains(propertyValue);
        }

        return false;
    }
}