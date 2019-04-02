/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.doclet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;
import com.sun.javadoc.TypeVariable;

/** Generates API definition for a package using the javadoc Doclet API */
public class ApiDoclet {
    private static final String OPTION_OUTPUT = "-output";
    private static final String OPTION_DOCTITLE = "-doctitle";
    private static final String OPTION_WINDOWTITLE = "-windowtitle";
    private static final String OPTION_DIRECTORY = "-d";
    private static final String OPTION_SKIP_CLASS_REGEX = "-skip-class-regex";

    private static final Set<String> ANNOTATIONS = new HashSet<>();
    static {
        ANNOTATIONS.add("java.lang.Deprecated");

        ANNOTATIONS.add("android.support.annotation.NonNull");
        ANNOTATIONS.add("android.support.annotation.Nullable");

        ANNOTATIONS.add("android.support.annotation.AnyThread");
        ANNOTATIONS.add("android.support.annotation.BinderThread");
        ANNOTATIONS.add("android.support.annotation.MainThread");
        ANNOTATIONS.add("android.support.annotation.UiThread");
        ANNOTATIONS.add("android.support.annotation.WorkerThread");
    }

    /** Doclet API: check that the options provided are valid */
    public static boolean validOptions(String options[][],
                                       DocErrorReporter reporter) {
        if (!parseOptions(options).isPresent()) {
            reporter.printError("Usage: javadoc -output api.txt ...");
        }

        return parseOptions(options).isPresent();
    }

    /** Doclet API: main entry point */
    public static boolean start(RootDoc root) {
        // The right options will always be present as they're checked in
        // `validOptions`
        final Options options = parseOptions(root.options()).get();

        try {
            final Writer writer = new WriterImpl(
                        new BufferedWriter(new FileWriter(options.outputFileName)));

            return writeApi(root, writer, options);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    private final List<Pattern> mSkipClasses;

    private ApiDoclet(List<Pattern> skipClasses) {
        mSkipClasses = skipClasses;
    }

    // public for testing
    public static boolean writeApi(RootDoc root, Writer writer, Options options) {
        final ApiDoclet instance = new ApiDoclet(options.skipClasses);

        sorted(root.specifiedPackages())
                .forEach(p -> instance.writePackage(p, writer));

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
        case OPTION_SKIP_CLASS_REGEX:
            return 2;
        default:
            return 0;
        }
    }

    /** Doclet API: needed for feture present in Java 5 an beyond like VarArg */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    private static class Options {
        final String outputFileName;
        final List<Pattern> skipClasses;

        public Options(String outputFileName, List<String> skipClassesRegex) {
            this.outputFileName = outputFileName;
            this.skipClasses = skipClassesRegex.stream()
                    .map(Pattern::compile)
                    .collect(Collectors.toList());
        }
    }

    private static Optional<Options> parseOptions(String options[][]) {
        Optional<String> outputFileName = Optional.empty();
        List<String> skipClasses = new ArrayList<>();

        for (int i = 0; i < options.length; i++) {
            String[] option = options[i];
            switch (option[0]) {
                case OPTION_OUTPUT:
                    outputFileName = Optional.of(option[1]);
                    break;
                case OPTION_SKIP_CLASS_REGEX:
                    skipClasses.add(option[1]);
                    break;
                default:
                    // ignore
            }
        }

        return outputFileName.map(output -> new Options(output, skipClasses));
    }

    private void writePackage(PackageDoc packageDoc, Writer writer) {
        writer.line("package " + packageDoc.name() + " {");
        writer.newLine();

        sorted(packageDoc.allClasses()).forEach(
                c -> writeClass(c, writer.indent()));

        writer.line("}");
        writer.newLine();
    }

    private static <T extends Doc> Stream<T> sorted(T[] items) {
        return Stream.of(items).sorted(DOC_COMPARATOR);
    }

    private static final Comparator<Doc> DOC_COMPARATOR = new Comparator<Doc>() {
        @Override
        public int compare(Doc o1, Doc o2) {
            if (o1 instanceof ProgramElementDoc && o2 instanceof ProgramElementDoc) {
                return compareProgramElement((ProgramElementDoc) o1, (ProgramElementDoc) o2);
            }

            return o1.name().compareTo(o2.name());
        }

        private int compareProgramElement(ProgramElementDoc o1, ProgramElementDoc o2) {
            // Sort public members before private methods
            if (o1.isPublic() != o2.isPublic()) {
                return o1.isPublic() ? -1 : 1;
            }

            // Then protected members
            if (o1.isProtected() != o2.isProtected()) {
                return o2.isProtected() ? -1 : 1;
            }

            // Otherwise sort by name
            return o1.name().compareTo(o2.name());
        }
    };

    static class AnnotationWalker implements Walker<AnnotationDesc> {
        @Override
        public List<AnnotationDesc> visit(ClassDoc classDoc) {
            return Arrays.asList(classDoc.annotations());
        }
    }

    interface Walker<T> {
        List<T> visit(ClassDoc classDoc);
    }

    private <T> List<T> collectHierarchy(ClassDoc classDoc, Walker<T> walker, List<T> collected) {
        collected.addAll(walker.visit(classDoc));

        if (classDoc.superclass() != null) {
            collectHierarchy(classDoc.superclass(), walker, collected);
        }

        return collected;
    }

    private <T> List<T> collectHierarchy(ClassDoc classDoc, Walker<T> walker) {
        return collectHierarchy(classDoc, walker, new ArrayList<T>());
    }

    private String toLine(ClassDoc classDoc, Writer writer) {
        String classLine = annotationFragment(
                collectHierarchy(classDoc, new AnnotationWalker()).stream(),
                writer);

        classLine += classDoc.modifiers() + " ";

        if (!classDoc.isInterface() && !classDoc.isEnum() &&
                !classDoc.isAnnotationType()) {
            classLine += "class ";
        } else if (classDoc.isEnum()) {
            classLine += "enum ";
        }

        classLine += classDoc.name();

        String typeParams = typeParamsFragment(classDoc.typeParameters());
        classLine += typeParams;

        if (typeParams.equals("")) {
            classLine += " ";
        }

        if (classDoc.superclass() != null
                // Ignore trivial superclass
                && !classDoc.superclass().toString().equals("java.lang.Object")
                && !classDoc.isEnum()) {
            classLine += "extends " + typeFragment(classDoc.superclass(), writer) + " ";
        }

        if (classDoc.interfaces().length > 0) {
            classLine += "implements " + sorted(classDoc.interfaces())
                .map(t -> typeFragment(t, writer))
                .collect(Collectors.joining(" "));
            classLine += " ";
        }

        classLine += "{";
        return classLine;
    }

    private void writeClass(ClassDoc classDoc, Writer writer) {
        if (mSkipClasses.stream()
                .anyMatch(sk -> sk.matcher(classDoc.qualifiedName()).find())) {
            return;
        }

        writer.line(toLine(classDoc, writer));

        Stream.<Stream<? extends ProgramElementDoc>> of(
                sorted(classDoc.constructors()),
                sorted(classDoc.methods())
                        // Don't add @Override methods to the API
                        .filter(m -> findSuperMethod(m, writer) == null),
                sorted(classDoc.enumConstants()),
                sorted(classDoc.fields()))
            .flatMap(s -> s.map(p -> toLine(p, writer)))
            .forEach(writer.indent()::line);

        writer.line("}");
        writer.newLine();
    }

    private Stream<String> from(Stream<AnnotationDesc> annotations, Writer writer) {
        return annotations.map(AnnotationDesc::annotationType)
                    .map(AnnotationTypeDoc::toString)
                    .filter(ANNOTATIONS::contains)
                    .map(s -> "@" + writer.import_(s));
    }

    private String annotationFragment(ProgramElementDoc member, Writer writer) {
        return annotationFragment(Stream.of(member.annotations()), writer);
    }

    private String annotationFragment(Stream<AnnotationDesc> annotations, Writer writer) {
        String fragment = from(annotations, writer)
                .distinct()
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

    private String parametrizedTypeFragment(ParameterizedType type, Writer writer) {
        String typeArgs =
                Stream.of(type.typeArguments())
                        .map(a -> typeFragment(a, writer))
                        .collect(Collectors.joining(","));

        String fragment = writer.import_(type.qualifiedTypeName());

        if (!typeArgs.equals("")) {
            fragment += "<" + typeArgs + ">";
        }

        return fragment + type.dimension();
    }

    private String typeFragment(Type type, Writer writer) {
        if (type.asParameterizedType() != null) {
            return parametrizedTypeFragment(type.asParameterizedType(), writer);
        }

        return writer.import_(type.qualifiedTypeName()) + type.dimension();
    }

    private String paramFragment(Parameter parameter, Writer writer) {
        return annotationFragment(Stream.of(parameter.annotations()), writer)
                + typeFragment(parameter.type(), writer);
    }

    private String paramsFragment(ExecutableMemberDoc executable, Writer writer) {
        String fragment = "(";

        fragment += Stream.of(executable.parameters())
                .map(p -> paramFragment(p, writer))
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

    private String typeParamsFragment(TypeVariable[] typeVariables) {
        String parameters = Stream.of(typeVariables)
                .map(TypeVariable::toString)
                .collect(Collectors.joining(","));

        if (parameters.equals("")) {
            return "";
        }

        return "<" + parameters + "> ";
    }

    private String typeParamsFragment(ExecutableMemberDoc executable) {
        return typeParamsFragment(executable.typeParameters());
    }

    static class MethodWalker implements Walker<MethodDoc> {
        public List<MethodDoc> visit(ClassDoc classDoc) {
            return Arrays.asList(classDoc.methods());
        }
    }

    static class InterfaceMethodWalker implements Walker<MethodDoc> {
        public List<MethodDoc> visit(ClassDoc classDoc) {
            return Stream.of(classDoc.interfaces())
                    .flatMap(i -> Stream.of(i.methods()))
                    .collect(Collectors.toList());
        }
    }

    private String typesFragment(ExecutableMemberDoc executable, Writer writer) {
        String fragment = "(";

        fragment += Stream.of(executable.parameters())
                .map(p -> p.type())
                .map(t -> typeFragment(t, writer))
                .collect(Collectors.joining(", "));

        if (executable.isVarArgs()) {
            fragment = fragment.replaceAll("\\[\\]$", "...");
        }

        return fragment + ")";
    }

    private ExecutableMemberDoc findSuperMethod(ExecutableMemberDoc member, Writer writer) {
        List<MethodDoc> methods = collectHierarchy(member.containingClass(),
                new InterfaceMethodWalker());

        // TODO: resolve all super classes not just the immediate one
        ClassDoc superClass = member.containingClass().superclass();
        if (superClass != null) {
            methods.addAll(collectHierarchy(superClass, new MethodWalker()));
        }

        return methods.stream()
                .filter(m -> m.name().equals(member.name())
                            && typesFragment(m, writer).equals(typesFragment(member, writer)))
                .findFirst()
                .orElse(null);
    }

    private String toLine(ProgramElementDoc member, Writer writer) {
        String line = tag(member) + " ";

        line += annotationFragment(Stream.of(member.annotations()), writer);

        if (member instanceof MethodDoc) {
            line += ((MethodDoc) member).isDefault() ? "default " : "";
        }

        if (!member.modifiers().equals("")) {
            line += member.modifiers() + " ";
        }

        if (member instanceof ExecutableMemberDoc) {
            line += typeParamsFragment((ExecutableMemberDoc) member);
        }

        if (member instanceof MethodDoc) {
            line += (typeFragment(((MethodDoc) member).returnType(), writer)) + " ";
        } else if (member instanceof FieldDoc){
            line += typeFragment(((FieldDoc) member).type(), writer) + " ";
        }

        line += member.name();

        if (member instanceof ExecutableMemberDoc) {
            line += paramsFragment((ExecutableMemberDoc) member, writer);
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
        String import_(String path);
    }

    private static class WriterImpl implements Writer {
        private final BufferedWriter mWriter;
        private final int mIndentation;
        private final StringBuilder mStringBuilder;
        private final Map<String, String> mImports;

        private static final String INDENTATION = "  ";

        public WriterImpl(BufferedWriter writer) {
            this(writer, 0, new StringBuilder(), new HashMap<>());
        }

        private WriterImpl(BufferedWriter writer, int indentation, StringBuilder stringBuilder,
                           Map<String, String> imports) {
            mWriter = writer;
            mIndentation = indentation;
            mStringBuilder = stringBuilder;
            mImports = imports;
        }

        public Writer indent() {
            return new WriterImpl(mWriter, mIndentation + 1, mStringBuilder, mImports);
        }

        public String import_(String path) {
            String[] split = path.split("\\.");
            if (split.length == 1) {
                // Primitive type, nothing to import
                return path;
            }

            int firstClassIndex = 1;
            while (firstClassIndex <= split.length &&
                    Character.isLowerCase(split[firstClassIndex-1].charAt(0))) {
                firstClassIndex++;
            }

            String base = split[firstClassIndex - 1];
            String imported = String.join(".", Arrays.copyOfRange(split, 0, firstClassIndex));

            if (mImports.containsKey(base) && !mImports.get(base).equals(imported)) {
                // Conflicting import
                return path;
            }

            mImports.put(base, imported);
            return String.join(".", Arrays.copyOfRange(split, firstClassIndex -1, split.length));
        }

        public void close() {
            try {
                List<String> imports = new ArrayList<>(mImports.values());
                Collections.sort(imports);

                for (String imported: imports) {
                    mWriter.write("import ");
                    mWriter.write(imported);
                    mWriter.write(";\n");
                }

                if (mImports.values().size() > 0) {
                    mWriter.write("\n");
                }

                mWriter.write(mStringBuilder.toString());
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
            for (int i = 0; i < indent; i++) {
                mStringBuilder.append(INDENTATION);
            }
            mStringBuilder.append(text + "\n");
        }
    }
}
