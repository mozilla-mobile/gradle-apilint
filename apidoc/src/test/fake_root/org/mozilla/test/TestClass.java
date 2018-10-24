/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.test;

/** Test class used in ApiDocletTest */
public class TestClass {
    public String testFieldWithoutValue;
    public String testFieldWithValue = "testValue";

    public final String testFinalField = "finalValue";
    public final static String testFinalStaticField = "finalStaticValue";

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

    public void testVoidMethod() {}
    public String testStringMethod() { return null; }

    public void testVoidMethodWithArg(String arg) {}
    public String testStringMethodWithArg(String arg) { return null; }

    public synchronized void testSynchronized() {}
    public static void testStatic() {}
    public final void testFinal() {}

    public void testVarArgsOneArg(int ... var1) {}
    public void testVarArgsTwoArgs(int var0, int ... var1) {}

    public void testFinalArg(final int arg) {}

    protected void testProtectedMethod() {}
    void testPackageProtectedMethod() {}
}
