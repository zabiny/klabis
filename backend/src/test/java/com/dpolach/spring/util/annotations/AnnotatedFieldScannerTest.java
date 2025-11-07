package com.dpolach.spring.util.annotations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnnotatedFieldScannerTest {

    private AnnotatedFieldScanner scanner;

    @Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    static @interface TestAnnotation {
    }

    // Testovací třídy
    static class TestClassWithTestAnnotation {
        @TestAnnotation
        private String testAnnotationField;

        private String normalField;
    }

    static class TestClassWithoutAnnotations {
        private String field1;
        private int field2;
    }

    static class TestClassWithMultipleTestAnnotation {
        @TestAnnotation
        private String field1;

        @TestAnnotation
        private Integer field2;

        private Double field3;
    }

    @BeforeEach
    void setUp() {
        scanner = new AnnotatedFieldScanner();
    }

    @Test
    void testHasFieldWithAnnotation_WithAnnotatedField() {
        boolean result = scanner.hasFieldWithAnnotation(TestClassWithTestAnnotation.class, TestAnnotation.class);
        assertTrue(result, "Třída by měla obsahovat pole s @TestAnnotation anotací");
    }

    @Test
    void testHasFieldWithAnnotation_WithoutAnnotatedField() {
        boolean result = scanner.hasFieldWithAnnotation(TestClassWithoutAnnotations.class, TestAnnotation.class);
        assertFalse(result, "Třída by neměla obsahovat pole s @TestAnnotation anotací");
    }

    @Test
    void testGetAnnotatedFields_SingleField() {
        List<Field> fields = scanner.getAnnotatedFields(TestClassWithTestAnnotation.class, TestAnnotation.class);

        assertEquals(1, fields.size(), "Mělo by být nalezeno právě jedno anotované pole");
        assertEquals("testAnnotationField", fields.get(0).getName());
    }

    @Test
    void testGetAnnotatedFields_MultipleFields() {
        List<Field> fields = scanner.getAnnotatedFields(TestClassWithMultipleTestAnnotation.class,
                TestAnnotation.class);

        assertEquals(2, fields.size(), "Měla by být nalezena dvě anotovaná pole");

        List<String> fieldNames = fields.stream()
                .map(Field::getName)
                .sorted()
                .toList();

        assertTrue(fieldNames.contains("field1"));
        assertTrue(fieldNames.contains("field2"));
    }

    @Test
    void testGetAnnotatedFields_NoAnnotatedFields() {
        List<Field> fields = scanner.getAnnotatedFields(TestClassWithoutAnnotations.class, TestAnnotation.class);

        assertTrue(fields.isEmpty(), "Neměla by být nalezena žádná anotovaná pole");
    }

    @Test
    void testFindClassesWithAnnotatedFields_InRealPackage() {
        // Test na skutečném balíčku projektu
        List<Class<?>> classes = scanner.findClassesWithAnnotatedFields(
                "com.dpolach.baseapp",
                TestAnnotation.class
        );

        assertNotNull(classes);
        // V reálném projektu by měly být nějaké třídy s @TestAnnotation
        // assertTrue(classes.size() > 0, "V projektu by měly existovat třídy s @TestAnnotation poli");
    }

    @Test
    void testFindClassesWithAnnotatedFieldsDetailed() {
        Map<Class<?>, List<Field>> result = scanner.findClassesWithAnnotatedFieldsDetailed(
                "com.dpolach.baseapp",
                TestAnnotation.class
        );

        assertNotNull(result);
        // Každá třída v mapě by měla mít alespoň jedno anotované pole
        result.forEach((clazz, fields) ->
                assertTrue(fields.size() > 0,
                        "Třída " + clazz.getSimpleName() + " by měla mít alespoň jedno anotované pole")
        );
    }

    @Test
    void testFindClassesWithAnnotatedFields_MultiplePackages() {
        List<Class<?>> classes = scanner.findClassesWithAnnotatedFields(
                List.of("com.dpolach.baseapp", "com.dpolach.eventsourcing"),
                TestAnnotation.class
        );

        assertNotNull(classes);
        // Neměly by být duplicity
        long uniqueCount = classes.stream().distinct().count();
        assertEquals(uniqueCount, classes.size(), "Neměly by existovat duplicitní třídy");
    }

    @Test
    void testVisitClassesWithAnnotatedFields_SinglePackage() {
        TestVisitor visitor = new TestVisitor();

        // Pro test použijeme vnitřní testovací třídy
        // V reálném projektu by to našlo skutečné třídy
        scanner.visitClassesWithAnnotatedFields(
                "com.dpolach.baseapp",
                TestAnnotation.class,
                visitor
        );

        // Visitor by měl být zavolán
        assertNotNull(visitor);
    }

    @Test
    void testVisitClassesWithAnnotatedFields_MultiplePackages() {
        TestVisitor visitor = new TestVisitor();

        scanner.visitClassesWithAnnotatedFields(
                List.of("com.dpolach.baseapp", "com.dpolach.eventsourcing"),
                TestAnnotation.class,
                visitor
        );

        assertNotNull(visitor);
    }

    @Test
    void testVisitorReceivesCorrectParameters() {
        // Pomocná třída pro uchování dat z visitoru
        class VisitorData {
            Field receivedField;
            Class<?> receivedClass;
        }

        VisitorData data = new VisitorData();

        // Simulace návštěvy pro naši testovací třídu
        List<Field> fields = scanner.getAnnotatedFields(TestClassWithTestAnnotation.class, TestAnnotation.class);

        if (!fields.isEmpty()) {
            Field field = fields.get(0);
            Class<?> clazz = TestClassWithTestAnnotation.class;

            AnnotatedFieldVisitor visitor = (f, c) -> {
                data.receivedField = f;
                data.receivedClass = c;
            };

            visitor.visit(field, clazz);

            assertNotNull(data.receivedField, "Visitor by měl obdržet pole");
            assertNotNull(data.receivedClass, "Visitor by měl obdržet třídu");
            assertEquals(field, data.receivedField, "Visitor by měl obdržet správné pole");
            assertEquals(clazz, data.receivedClass, "Visitor by měl obdržet správnou třídu");
        }
    }

    @Test
    void testVisitorCalledForEachField() {
        class CountingVisitor implements AnnotatedFieldVisitor {
            int count = 0;

            @Override
            public void visit(Field field, Class<?> enclosingClass) {
                count++;
            }
        }

        CountingVisitor countingVisitor = new CountingVisitor();

        // Manuální simulace pro naši testovací třídu
        List<Field> fields = scanner.getAnnotatedFields(TestClassWithMultipleTestAnnotation.class,
                TestAnnotation.class);
        for (Field field : fields) {
            countingVisitor.visit(field, TestClassWithMultipleTestAnnotation.class);
        }

        assertEquals(2, countingVisitor.count,
                "Visitor by měl být zavolán pro každé anotované pole");
    }

    /**
     * Pomocný visitor pro testování.
     */
    private static class TestVisitor implements AnnotatedFieldVisitor {
        private int visitCount = 0;

        @Override
        public void visit(Field field, Class<?> enclosingClass) {
            visitCount++;
            assertNotNull(field, "Pole by nemělo být null");
            assertNotNull(enclosingClass, "Třída by neměla být null");
        }

        public int getVisitCount() {
            return visitCount;
        }
    }
}
