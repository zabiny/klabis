package com.dpolach.spring.util.annotations;

import java.lang.reflect.Field;

/**
 * Funkční interface pro visitor pattern při procházení anotovaných polí.
 * Visitor je volán pro každé pole s danou anotací.
 */
@FunctionalInterface
public interface AnnotatedFieldVisitor {

    /**
     * Metoda volaná pro každé nalezené pole s anotací.
     *
     * @param field          anotované pole
     * @param enclosingClass třída, která obsahuje dané pole
     */
    void visit(Field field, Class<?> enclosingClass);
}
