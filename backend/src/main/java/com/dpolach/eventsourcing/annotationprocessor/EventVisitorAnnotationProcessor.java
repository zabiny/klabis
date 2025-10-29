package com.dpolach.eventsourcing.annotationprocessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes("com.example.eventstore.annotations.Event")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
//@AutoService(Processor.class)
public class EventVisitorAnnotationProcessor extends AbstractProcessor {

    private static final String PACKAGE_NAME = "com.example.eventstore";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Generating DomainEventVisitor");
        try {
            StringBuilder interfaceBuilder = new StringBuilder();
            interfaceBuilder.append("package ").append(PACKAGE_NAME).append(";\n\n");
            interfaceBuilder.append("public interface DomainEventVisitor<T> {\n");

            StringBuilder abstractClassBuilder = new StringBuilder();
            abstractClassBuilder.append("package ").append(PACKAGE_NAME).append(";\n\n");
            abstractClassBuilder.append(
                    "public abstract class AbstractDomainEventVisitor<T> implements DomainEventVisitor<T> {\n");

            for (Element element : roundEnv.getElementsAnnotatedWith(processingEnv.getElementUtils()
                    .getTypeElement("com.example.eventstore.annotations.Event"))) {
                String className = ((TypeElement) element).getQualifiedName().toString();

                interfaceBuilder.append("    T visit(").append(className).append(" event);\n");

                abstractClassBuilder.append("    @Override\n");
                abstractClassBuilder.append("    public T visit(").append(className).append(" event) {\n");
                abstractClassBuilder.append("        return null;\n");
                abstractClassBuilder.append("    }\n");
            }

            interfaceBuilder.append("}\n");
            abstractClassBuilder.append("}\n");

            JavaFileObject interfaceFile = processingEnv.getFiler()
                    .createSourceFile(PACKAGE_NAME + ".DomainEventVisitor");
            try (Writer writer = interfaceFile.openWriter()) {
                writer.write(interfaceBuilder.toString());
            }

            JavaFileObject abstractClassFile = processingEnv.getFiler()
                    .createSourceFile(PACKAGE_NAME + ".AbstractDomainEventVisitor");
            try (Writer writer = abstractClassFile.openWriter()) {
                writer.write(abstractClassBuilder.toString());
            }

        } catch (Exception e) {
            processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Error generating visitor: " + e.getMessage());
            return false;
        }
        return true;
    }
}
