/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.test;

import java.util.List;

/** Test class used in ApiDocletTest */
public class TestClass {
    public String testFieldWithoutValue;
    public String testFieldWithValue = "testValue";

    public final String testFinalField = "finalValue";
    public final static String testFinalStaticField = "finalStaticValue";

    public final static List<String> TEST_COMPOSITE_TYPE = null;
    public final static List<List<String>> TEST_NESTED_COMPOSITE_TYPE = null;

    protected int testProtectedField;
    int testPackageProtectedField;

    public class TestSubclass {}
    public static class TestStaticSubclass {}
    public abstract class TestAbstractClass {}
    public interface TestInterface {
        void testInterfaceMethod();
    }
    public interface TestInterfaceTwo {}

    public static class TestInterfaceImpl implements TestInterface {
        public void testInterfaceMethod() {}
    }
    public static class TestMultipleInterfaceImpl implements TestInterface, TestInterfaceTwo {
        public void testInterfaceMethod() {}
    }
    public static class TestExtends extends TestInterfaceImpl {}

    public TestClass() {}
    public TestClass(String arg1) {}
    public TestClass(String arg1, int arg2) {}
    protected TestClass (boolean arg) {}
    TestClass(int arg) {}

    @Deprecated
    public class DeprecatedClass {}
    @SuppressWarnings("")
    public class HiddenAnnotationClass {}

    public void testVoidMethod() {}
    public String testStringMethod() { return null; }

    public void testVoidMethodWithArg(String arg) {}
    public String testStringMethodWithArg(String arg) { return null; }

    public synchronized void testSynchronized() {}
    public static void testStatic() {}
    public final void testFinal() {}

    public void testVarArgsOneArg(int ... var1) {}
    public void testVarArgsTwoArgs(int var0, int ... var1) {}

    public void testArrayArg(int[] var) {}

    public void testFinalArg(final int arg) {}

    protected void testProtectedMethod() {}
    void testPackageProtectedMethod() {}

    @Deprecated
    public void testAnnotation() {}
    @Deprecated
    public TestClass(float arg) {}
    @Deprecated
    public final static int TEST_DEPRECATED_CONST = 1;

    @SuppressWarnings("")
    public void testHiddenAnnotation() {}
    @SuppressWarnings("")
    public TestClass(int arg0, float arg1) {}
    @SuppressWarnings("")
    public final static int TEST_HIDDEN_ANNOTATION = 2;

    public @interface TestAnnotation {}

    public <T> void testTypeVariableUnbounded(T arg) {}
    public <T extends java.lang.Runnable> void testTypeVariableWithBounds(T arg) {}
    public <T extends java.lang.Runnable & java.lang.Cloneable> void testTypeVariableWithMultipleBounds(T arg) {}

    public <T> T testReturnTypeUnbounded() { return null; }
    public <T extends java.lang.Runnable> T testReturnTypeWithBound() { return null; }

    public <T> List<List<T>> testReturnNestedCompositeType() { return null; }
    public <T> void testParamNestedCompositeType(List<List<T>> arg) {}

    public <T> List<T> testReturnCompositeType() { return null; }
    public <T> void testCompositeParam(List<T> arg) {}

    public static class TestTypeVariable<T> {
        public void testTypeVariableMethod(T arg);
    }

    public static class TestTypeBoundVariable<T extends java.lang.Runnable> {
        public void testTypeVariableMethod(T arg);
    }

    public static interface TestInterfaceTypeVariable<T> {
        public void testTypeVariableMethod(T arg);
    }

    public static enum TestEnum {
        TestEnumConstantOne,
        TestEnumConstantTwo,
    }

    public static class TestSort {
        private TestSort() {}

        // Test sorting fields by name
        public final static int TEST_SORT_C = 1;
        public final static int TEST_SORT_A = 2;
        public final static int TEST_SORT_D = 3;
        public final static int TEST_SORT_B = 4;

        // Test sorting methods by name
        public void testSortD0();
        public void testSortA0();
        public void testSortC0();
        public void testSortB0();

        // Test that protected methods come after public
        protected void testSortD1();
        protected void testSortA1();
        protected void testSortC1();
        protected void testSortB1();

        // Test sorting classes by name
        public static class TestSortA {
            private TestSortA() {}
        }
        public static class TestSortD {
            private TestSortD() {}
        }
        public static class TestSortB {
            private TestSortB() {}
        }
        public static class TestSortC {
            private TestSortC() {}
        }
    }
}
