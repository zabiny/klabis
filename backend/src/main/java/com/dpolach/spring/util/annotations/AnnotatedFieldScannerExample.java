package com.dpolach.spring.util.annotations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Příklad použití AnnotatedFieldScanner.
 * Tato komponenta se spustí při startu aplikace a demonstrovat použití scanneru.
 * <p>
 * Pro aktivaci odkomentujte @Component anotaci.
 */
// @Component
public class AnnotatedFieldScannerExample implements CommandLineRunner {

    private final AnnotatedFieldScanner scanner;

    @Autowired
    public AnnotatedFieldScannerExample(AnnotatedFieldScanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void run(String... args) throws Exception {
        // Příklad 1: Najít všechny třídy s @Autowired poli
        System.out.println("=== Příklad 1: Třídy s @Autowired poli ===");
        List<Class<?>> classesWithAutowired = scanner.findClassesWithAnnotatedFields(
                "com.dpolach.baseapp",
                Autowired.class
        );

        classesWithAutowired.forEach(clazz ->
                System.out.println("Třída: " + clazz.getSimpleName())
        );

        // Příklad 2: Detailní informace o třídách a jejich anotovaných polích
        System.out.println("\n=== Příklad 2: Detailní informace ===");
        Map<Class<?>, List<Field>> detailedInfo = scanner.findClassesWithAnnotatedFieldsDetailed(
                "com.dpolach.baseapp",
                Autowired.class
        );

        detailedInfo.forEach((clazz, fields) -> {
            System.out.println("Třída: " + clazz.getSimpleName());
            fields.forEach(field ->
                    System.out.println("  - Pole: " + field.getName() + " (" + field.getType().getSimpleName() + ")")
            );
        });

        // Příklad 3: Vyhledávání ve více balíčcích
        System.out.println("\n=== Příklad 3: Vyhledávání ve více balíčcích ===");
        List<Class<?>> classesFromMultiplePackages = scanner.findClassesWithAnnotatedFields(
                List.of("com.dpolach.baseapp", "com.dpolach.eventsourcing"),
                Autowired.class
        );

        System.out.println("Celkem nalezeno tříd: " + classesFromMultiplePackages.size());

        // Příklad 4: Použití visitor patternu
        System.out.println("\n=== Příklad 4: Visitor pattern ===");
        scanner.visitClassesWithAnnotatedFields(
                "com.dpolach.baseapp",
                Autowired.class,
                (field, enclosingClass) -> {
                    System.out.println(String.format(
                            "Třída: %s, Pole: %s, Typ: %s",
                            enclosingClass.getSimpleName(),
                            field.getName(),
                            field.getType().getSimpleName()
                    ));
                }
        );

        // Příklad 5: Visitor pattern s vlastní logikou
        System.out.println("\n=== Příklad 5: Visitor pattern s počítáním ===");
        FieldCounter counter = new FieldCounter();
        scanner.visitClassesWithAnnotatedFields(
                "com.dpolach.baseapp",
                Autowired.class,
                counter
        );
        System.out.println("Celkem nalezeno anotovaných polí: " + counter.getCount());
    }

    /**
     * Příklad vlastního visitoru pro počítání polí.
     */
    private static class FieldCounter implements AnnotatedFieldVisitor {
        private int count = 0;

        @Override
        public void visit(Field field, Class<?> enclosingClass) {
            count++;
        }

        public int getCount() {
            return count;
        }
    }
}
