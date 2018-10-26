/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.doclet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

/** Generates API definition for a package using the javadoc Doclet API */
public class ApiDoclet {
    private static final String OPTION_OUTPUT = "-output";
    private static final String OPTION_DOCTITLE = "-doctitle";
    private static final String OPTION_WINDOWTITLE = "-windowtitle";
    private static final String OPTION_DIRECTORY = "-d";

    private static final Set<String> ANNOTATIONS = new HashSet<>();
    static {
        ANNOTATIONS.add("java.lang.Deprecated");

        ANNOTATIONS.add("android.support.annotation.AnyThread");
        ANNOTATIONS.add("android.support.annotation.BinderThread");
        ANNOTATIONS.add("android.support.annotation.MainThread");
        ANNOTATIONS.add("android.support.annotation.UiThread");
        ANNOTATIONS.add("android.support.annotation.WorkerThread");
    }

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
        String classLine = annotationFragment(classDoc);
        classLine += classDoc.modifiers() + " ";

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

        Stream.<Stream<ProgramElementDoc>> of(
                Stream.of(classDoc.constructors()),
                Stream.of(classDoc.methods()),
                Stream.of(classDoc.enumConstants()),
                Stream.of(classDoc.fields()))
            .flatMap(s -> s.map(this::toLine))
            .forEach(writer.indent()::line);

        writer.line("}");
        writer.newLine();
    }

    private Stream<String> from(AnnotationDesc[] annotation) {
        return Stream.of(annotation)
                    .map(AnnotationDesc::annotationType)
                    .map(AnnotationTypeDoc::toString)
                    .filter(ANNOTATIONS::contains)
                    .map(s -> "@" + s);
    }

    private String annotationFragment(ProgramElementDoc member) {
        String fragment = from(member.annotations())
               .collect(Collectors.joining(" "));
        if (fragment.equals("")) {
            return "";
        }
        return fragment + " ";
    }

    private String tag(ProgramElementDoc member) {
        if (member.isConstructor()) {
            return "ctor";
        } else if (member.isMethod()) {
            return "method";
        } else if (member.isField()) {
            return "field";
        } else if (member.isEnumConstant()) {
            return "enum_constant";
        } else {
            throw new IllegalArgumentException("Unexpected member.");
        }
    }

    private String paramsFragment(ExecutableMemberDoc executable) {
        String fragment = "(";

        fragment += Stream.of(executable.parameters())
                .map(Parameter::type)
                .map(Type::toString)
                .collect(Collectors.joining(", "));

        if (executable.isVarArgs()) {
            fragment = fragment.replaceAll("\\[\\]$", "...");
        }

        return fragment + ")";
    }

    private String valueFragment(FieldDoc field) {
        if (field.constantValueExpression() == null) {
            return "";
        }

        return " = " + field.constantValueExpression();
    }

    private String toLine(ProgramElementDoc member) {
        String line = tag(member) + " ";

        line += annotationFragment(member);

        if (!member.modifiers().equals("")) {
            line += member.modifiers() + " ";
        }

        if (member instanceof MethodDoc) {
            line += ((MethodDoc) member).returnType() + " ";
        } else if (member instanceof FieldDoc){
            line += ((FieldDoc) member).type() + " ";
        }

        line += member.name();

        if (member instanceof ExecutableMemberDoc) {
            line += paramsFragment((ExecutableMemberDoc) member);
        }

        if (member instanceof FieldDoc) {
            line += valueFragment((FieldDoc) member);
        }

        return line + ";";
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
