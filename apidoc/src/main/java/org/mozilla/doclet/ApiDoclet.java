/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.doclet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

/** Generates API definition for a package using the javadoc Doclet API */
public class ApiDoclet {
    private static final String OPTION_OUTPUT = "-output";
    private static final String OPTION_DOCTITLE = "-doctitle";
    private static final String OPTION_WINDOWTITLE = "-windowtitle";
    private static final String OPTION_DIRECTORY = "-d";

    /** Doclet API: check that the options provided are valid */
    public static boolean validOptions(String options[][],
                                       DocErrorReporter reporter) {
        if (!outputFileName(options).isPresent()) {
            reporter.printError("Usage: javadoc -output api.txt ...");
        }

        return outputFileName(options).isPresent();
    }

    /** Doclet API: main entry point */
    public static boolean start(RootDoc root) {
        // The right options will always be present as they're checked in
        // `validOptions`
        final String fileName = outputFileName(root.options()).get();

        try {
            final Writer writer = new WriterImpl(
                        new BufferedWriter(new FileWriter(fileName)));

            return writeApi(root, writer);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    // public for testing
    public static boolean writeApi(RootDoc root, Writer writer) {
        final ApiDoclet instance = new ApiDoclet();

        Stream.of(root.specifiedPackages()).forEach(
                p -> instance.writePackage(p, writer));

        writer.close();

        return true;
    }

    /** Doclet API: get number of arguments for a given option */
    public static int optionLength(String option) {
        switch (option) {
        case OPTION_OUTPUT:
        case OPTION_DIRECTORY:
        case OPTION_DOCTITLE:
        case OPTION_WINDOWTITLE:
            return 2;
        default:
            return 0;
        }
    }

    /** Doclet API: needed for feture present in Java 5 an beyond like VarArg */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    private static Optional<String> outputFileName(String options[][]) {
        return Stream.of(options)
            .filter(option -> OPTION_OUTPUT.equals(option[0]))
            // option[1] is the value of the option
            .map(option -> option[1])
            .findFirst();
    }

    private void writePackage(PackageDoc packageDoc, Writer writer) {
        writer.line("package " + packageDoc.name() + " {");
        writer.newLine();

        sorted(packageDoc.allClasses()).forEach(
                c -> writeClass(c, writer.indent()));

        sorted(packageDoc.enums()).forEach(
                c -> writeClass(c, writer.indent()));

        writer.line("}");
        writer.newLine();
    }

    private static <T extends Doc> Stream<T> sorted(T[] items) {
        return Stream.of(items).sorted(DOC_COMPARATOR);
    }

    private static final Comparator<Doc> DOC_COMPARATOR =
        new Comparator<Doc>() {
        @Override
        public int compare(Doc o1, Doc o2) {
            return o1.name().compareTo(o2.name());
        }
    };

    private String toLine(ClassDoc classDoc) {
        String classLine = classDoc.modifiers() + " ";
        if (!classDoc.isInterface()) {
            classLine += "class ";
        }
        classLine += classDoc.name();
        if (classDoc.superclass() != null
                // Ignore trivial superclass
                && !classDoc.superclass().toString().equals("java.lang.Object")) {
            classLine += " extends " + classDoc.superclass();
        }
        if (classDoc.interfaces().length > 0) {
            classLine += " implements " + sorted(classDoc.interfaces())
                .map(ClassDoc::toString)
                .collect(Collectors.joining(" "));
        }

        classLine += " {";
        return classLine;
    }

    private void writeClass(ClassDoc classDoc, Writer writer) {
        writer.line(toLine(classDoc));

        Stream.of(
                Stream.of(classDoc.constructors()).map(this::toLine),
                Stream.of(classDoc.methods()).map(this::toLine),
                Stream.of(classDoc.enumConstants()).map(this::toLineEnum),
                Stream.of(classDoc.fields()).map(this::toLine))
            .flatMap(s -> s)
            .forEach(writer.indent()::line);

        writer.line("}");
        writer.newLine();
    }

    private String toLineEnum(FieldDoc enumConstant) {
        return toLineBase("enum_constant", enumConstant);
    }

    private String toLine(ConstructorDoc constructor) {
        String constructorLine = "ctor ";
        if (!constructor.modifiers().equals("")) {
            constructorLine += constructor.modifiers() + " ";
        }
        constructorLine += constructor.name() + "(";

        constructorLine += from(constructor.parameters())
            .collect(Collectors.joining(", "));

        constructorLine += ");";

        return constructorLine;
    }

    private String toLine(MethodDoc method) {
        String methodLine = "method ";
        if (!method.modifiers().equals("")) {
            methodLine += method.modifiers() + " ";
        }
        methodLine += method.returnType() + " ";
        methodLine += method.name() + "(";

        Stream<String> parameters = from(method.parameters());

        methodLine += parameters
                .collect(Collectors.joining(", "));

        if (method.isVarArgs()) {
            methodLine = methodLine.replaceAll("\\[\\]$", "...");
        }

        methodLine += ");";
        return methodLine;
    }

    private String toLineBase(String tag, FieldDoc field) {
        String fieldLine = tag + " ";
        if (!field.modifiers().equals("")) {
            fieldLine += field.modifiers() + " ";
        }
        fieldLine += field.type() + " ";
        fieldLine += field.name();
        if (field.constantValueExpression() != null) {
            fieldLine += " = " + field.constantValueExpression();
        }

        fieldLine += ";";

        return fieldLine;
    }

    private String toLine(FieldDoc field) {
        return toLineBase("field", field);
    }

    private Stream<String> from(Parameter[] parameters) {
        return Stream.of(parameters)
            .map(Parameter::type)
            .map(Type::toString);
    }

    public interface Writer {
        Writer indent();
        void newLine();
        void line(String text);
        void close();
    }

    private static class WriterImpl implements Writer {
        private final BufferedWriter mWriter;
        private final int mIndentation;

        private static final String INDENTATION = "  ";

        public WriterImpl(BufferedWriter writer) {
            this(writer, 0);
        }

        private WriterImpl(BufferedWriter writer, int indentation) {
            mWriter = writer;
            mIndentation = indentation;
        }

        public Writer indent() {
            return new WriterImpl(mWriter, mIndentation + 1);
        }

        public void close() {
            try {
                mWriter.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        public void newLine() {
            line("", 0);
        }

        public void line(String text) {
            line(text, mIndentation);
        }

        private void line(String text, int indent) {
            try {
                for (int i = 0; i < indent; i++) {
                    mWriter.write(INDENTATION);
                }
                mWriter.write(text + "\n");
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
