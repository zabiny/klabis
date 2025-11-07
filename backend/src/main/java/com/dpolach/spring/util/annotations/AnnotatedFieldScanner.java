package com.dpolach.spring.util.annotations;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility třída pro vyhledávání tříd obsahujících pole anotovaná danou anotací.
 * Využívá Spring ClassPath scanning pro efektivní prohledávání balíčků.
 */
@Component
public class AnnotatedFieldScanner {

    /**
     * Najde všechny třídy v daném balíčku, které obsahují pole anotované danou anotací.
     *
     * @param basePackage základní balíček pro skenování
     * @param annotation  anotace, kterou hledáme na polích
     * @return seznam tříd obsahujících pole s danou anotací
     */
    public List<Class<?>> findClassesWithAnnotatedFields(String basePackage, Class<? extends Annotation> annotation) {
        Set<Class<?>> classesWithAnnotatedFields = new HashSet<>();

        // Vytvoření scanneru pro prohledávání classpath
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        // Přidání filtru pro všechny třídy
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

        // Najít všechny kandidáty v balíčku
        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);

        for (BeanDefinition bd : candidateComponents) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                if (hasFieldWithAnnotation(clazz, annotation)) {
                    classesWithAnnotatedFields.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                // Logování chyby, pokud třídu nelze načíst
                System.err.println("Nelze načíst třídu: " + bd.getBeanClassName());
            }
        }

        return new ArrayList<>(classesWithAnnotatedFields);
    }

    /**
     * Zkontroluje, zda třída obsahuje pole s danou anotací.
     *
     * @param clazz      třída ke kontrole
     * @param annotation hledaná anotace
     * @return true pokud třída obsahuje alespoň jedno pole s danou anotací
     */
    public boolean hasFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
        return getAnnotatedFields(clazz, annotation).size() > 0;
    }

    /**
     * Získá všechna pole třídy, která jsou anotována danou anotací.
     * Prohledává i pole v nadtřídách.
     *
     * @param clazz      třída ke kontrole
     * @param annotation hledaná anotace
     * @return seznam anotovaných polí
     */
    public List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        List<Field> annotatedFields = new ArrayList<>();

        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Field[] fields = currentClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(annotation)) {
                    annotatedFields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return annotatedFields;
    }

    /**
     * Najde všechny třídy s anotovanými poli a vrátí mapu třída -> seznam anotovaných polí.
     *
     * @param basePackage základní balíček pro skenování
     * @param annotation  anotace, kterou hledáme na polích
     * @return mapa tříd a jejich anotovaných polí
     */
    public Map<Class<?>, List<Field>> findClassesWithAnnotatedFieldsDetailed(
            String basePackage,
            Class<? extends Annotation> annotation) {

        List<Class<?>> classes = findClassesWithAnnotatedFields(basePackage, annotation);

        return classes.stream()
                .collect(Collectors.toMap(
                        clazz -> clazz,
                        clazz -> getAnnotatedFields(clazz, annotation)
                ));
    }

    /**
     * Najde všechny třídy ve více balíčcích.
     *
     * @param basePackages seznam balíčků pro skenování
     * @param annotation   hledaná anotace
     * @return seznam všech nalezených tříd (bez duplicit)
     */
    public List<Class<?>> findClassesWithAnnotatedFields(
            List<String> basePackages,
            Class<? extends Annotation> annotation) {

        Set<Class<?>> allClasses = new HashSet<>();

        for (String basePackage : basePackages) {
            allClasses.addAll(findClassesWithAnnotatedFields(basePackage, annotation));
        }

        return new ArrayList<>(allClasses);
    }

    /**
     * Navštíví všechna pole s danou anotací v zadaném balíčku pomocí visitor patternu.
     * Pro každé nalezené anotované pole zavolá visitor s polem a jeho obsahující třídou.
     *
     * @param basePackage základní balíček pro skenování
     * @param annotation  anotace, kterou hledáme na polích
     * @param visitor     visitor, který bude zavolán pro každé nalezené pole
     */
    public void visitClassesWithAnnotatedFields(
            String basePackage,
            Class<? extends Annotation> annotation,
            AnnotatedFieldVisitor visitor) {

        List<Class<?>> classes = findClassesWithAnnotatedFields(basePackage, annotation);

        for (Class<?> clazz : classes) {
            List<Field> annotatedFields = getAnnotatedFields(clazz, annotation);
            for (Field field : annotatedFields) {
                visitor.visit(field, clazz);
            }
        }
    }

    /**
     * Navštíví všechna pole s danou anotací ve více balíčcích pomocí visitor patternu.
     * Pro každé nalezené anotované pole zavolá visitor s polem a jeho obsahující třídou.
     *
     * @param basePackages seznam balíčků pro skenování
     * @param annotation   anotace, kterou hledáme na polích
     * @param visitor      visitor, který bude zavolán pro každé nalezené pole
     */
    public void visitClassesWithAnnotatedFields(
            List<String> basePackages,
            Class<? extends Annotation> annotation,
            AnnotatedFieldVisitor visitor) {

        Set<Class<?>> allClasses = new HashSet<>();

        for (String basePackage : basePackages) {
            allClasses.addAll(findClassesWithAnnotatedFields(basePackage, annotation));
        }

        for (Class<?> clazz : allClasses) {
            List<Field> annotatedFields = getAnnotatedFields(clazz, annotation);
            for (Field field : annotatedFields) {
                visitor.visit(field, clazz);
            }
        }
    }
}
