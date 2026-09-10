/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.errorprone.noreflection;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

/**
 * The categories, constructors and method references, what the flags allow and forbid, suppression, and the strict
 * check. Micronaut's own types are stood in for by stubs of the same names, so the checker needs no Micronaut.
 */
class NoReflectionTest {

    private static final String[] MICRONAUT_STUBS = {
        "io/micronaut/core/annotation/AnnotationSource.java",
        """
        package io.micronaut.core.annotation;
        public interface AnnotationSource {
            default <T extends java.lang.annotation.Annotation> T synthesize(Class<T> type) { return null; }
        }
        """,
        "io/micronaut/core/annotation/AnnotationMetadata.java",
        """
        package io.micronaut.core.annotation;
        public interface AnnotationMetadata extends AnnotationSource {
            default Object getAnnotation(String name) { return null; }
            default Object getDeclaredAnnotation(String name) { return null; }
        }
        """,
        "io/micronaut/inject/MethodReference.java",
        """
        package io.micronaut.inject;
        public interface MethodReference {
            java.lang.reflect.Method getTargetMethod();
        }
        """,
        "io/micronaut/inject/ExecutableMethod.java",
        """
        package io.micronaut.inject;
        public interface ExecutableMethod extends MethodReference {
        }
        """,
        "io/micronaut/inject/FieldInjectionPoint.java",
        """
        package io.micronaut.inject;
        public interface FieldInjectionPoint {
            java.lang.reflect.Field getField();
        }
        """,
        "io/micronaut/core/reflect/ReflectionUtils.java",
        """
        package io.micronaut.core.reflect;
        public final class ReflectionUtils {
            public static java.lang.reflect.Method getRequiredMethod(Class<?> type, String name) { return null; }
            public static Class<?> getWrapperType(Class<?> type) { return type; }
        }
        """,
        "io/micronaut/core/reflect/ClassUtils.java",
        """
        package io.micronaut.core.reflect;
        public final class ClassUtils {
            public static java.util.Optional<Class<?>> forName(String name, ClassLoader loader) { return java.util.Optional.empty(); }
            public static boolean isJavaLangType(Class<?> type) { return false; }
        }
        """,
        "io/micronaut/core/reflect/InstantiationUtils.java",
        """
        package io.micronaut.core.reflect;
        public final class InstantiationUtils {
            public static <T> T instantiate(Class<T> type) { return null; }
        }
        """,
        "io/micronaut/core/reflect/GenericTypeUtils.java",
        """
        package io.micronaut.core.reflect;
        public final class GenericTypeUtils {
            public static Class<?>[] resolveInterfaceTypeArguments(Class<?> type, Class<?> interfaceType) { return null; }
        }
        """,
        "io/micronaut/core/io/service/SoftServiceLoader.java",
        """
        package io.micronaut.core.io.service;
        public final class SoftServiceLoader<S> {
            public static <S> SoftServiceLoader<S> load(Class<S> service) { return null; }
        }
        """
    };

    private static CompilationTestHelper helper() {
        CompilationTestHelper helper = CompilationTestHelper.newInstance(NoReflection.class, NoReflectionTest.class);
        for (int i = 0; i < MICRONAUT_STUBS.length; i += 2) {
            helper = helper.addSourceLines(MICRONAUT_STUBS[i], MICRONAUT_STUBS[i + 1]);
        }
        return helper;
    }

    /** One call of each category, each reported under its own name. */
    private static final String EVERY_CATEGORY = """
        import io.micronaut.core.annotation.AnnotationMetadata;
        import io.micronaut.core.io.service.SoftServiceLoader;
        import io.micronaut.core.reflect.ReflectionUtils;
        import io.micronaut.inject.ExecutableMethod;
        class Subject {
            enum Colour { RED }
            void reflect(Class<?> type, AnnotationMetadata metadata, ExecutableMethod method, java.lang.reflect.Method member,
                         java.io.ObjectInputStream in, java.lang.instrument.Instrumentation instrumentation) throws Throwable {
                // BUG: Diagnostic contains: [ANNOTATION_SYNTHESIS]
                metadata.synthesize(Deprecated.class);
                // BUG: Diagnostic contains: [TARGET_MEMBERS]
                method.getTargetMethod();
                // BUG: Diagnostic contains: [REFLECTION_UTILS]
                ReflectionUtils.getRequiredMethod(type, "x");
                // BUG: Diagnostic contains: [BEANS]
                java.beans.Introspector.getBeanInfo(type);
                // BUG: Diagnostic contains: [SERIALIZATION]
                in.readObject();
                // BUG: Diagnostic contains: [UNSAFE]
                sun.misc.Unsafe.getUnsafe();
                // BUG: Diagnostic contains: [INSTRUMENTATION]
                instrumentation.getAllLoadedClasses();
                // BUG: Diagnostic contains: [PROXY]
                java.lang.reflect.Proxy.newProxyInstance(null, new Class<?>[0], null);
                // BUG: Diagnostic contains: [HANDLES]
                java.lang.invoke.MethodType.methodType(void.class);
                // BUG: Diagnostic contains: [SERVICE_LOADING]
                SoftServiceLoader.load(Runnable.class);
                // BUG: Diagnostic contains: [CLASS_LOADING]
                Class.forName("java.lang.String");
                // BUG: Diagnostic contains: [FIELD_UPDATERS]
                java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater(Subject.class, Object.class, "x");
                // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                Colour.valueOf("RED");
                // BUG: Diagnostic contains: [CLASS_NAMES]
                type.getSimpleName();
                // BUG: Diagnostic contains: [INTERFACES]
                type.getInterfaces();
                // BUG: Diagnostic contains: [GENERIC_SIGNATURES]
                type.getGenericSuperclass();
                // BUG: Diagnostic contains: [ANNOTATIONS]
                type.getAnnotation(Deprecated.class);
                // BUG: Diagnostic contains: [CLASS_MEMBERS]
                type.getDeclaredMethods();
                // BUG: Diagnostic contains: [REFLECTIVE_ACCESS]
                member.invoke(null);
            }
        }
        """;

    @Test
    void reportsEveryCategoryUnderItsName() {
        helper().addSourceLines("Subject.java", EVERY_CATEGORY).doTest();
    }

    /** The calls whose purpose is a cache the virtual machine fills for a class or a member, constructors and references included. */
    @Test
    void reportsTheCallsThatFillACacheOfTheVirtualMachine() {
        helper().addSourceLines("Subject.java", """
            import java.util.EnumMap;
            import java.util.EnumSet;
            import java.util.function.Function;
            import io.micronaut.core.reflect.ClassUtils;
            import io.micronaut.core.reflect.GenericTypeUtils;
            import io.micronaut.core.reflect.InstantiationUtils;
            import io.micronaut.inject.FieldInjectionPoint;
            class Subject {
                enum Colour { RED }
                void reflect(Class<?> type, java.lang.reflect.Method method, java.lang.reflect.Field field,
                             java.util.ServiceLoader.Provider<Runnable> provider, FieldInjectionPoint point) throws Exception {
                    // BUG: Diagnostic contains: [CLASS_NAMES]
                    type.getCanonicalName();
                    // BUG: Diagnostic contains: [CLASS_MEMBERS]
                    type.getEnclosingMethod();
                    // BUG: Diagnostic contains: [CLASS_MEMBERS]
                    method.getParameters();
                    // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                    type.getEnumConstants();
                    // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                    Enum.valueOf(Colour.class, "RED");
                    // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                    EnumSet.noneOf(Colour.class);
                    // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                    new EnumMap<Colour, String>(Colour.class);
                    // BUG: Diagnostic contains: [GENERIC_SIGNATURES]
                    field.getGenericType();
                    // BUG: Diagnostic contains: [GENERIC_SIGNATURES]
                    method.getGenericReturnType();
                    // BUG: Diagnostic contains: [GENERIC_SIGNATURES]
                    GenericTypeUtils.resolveInterfaceTypeArguments(type, Runnable.class);
                    // BUG: Diagnostic contains: [ANNOTATIONS]
                    method.getParameterAnnotations();
                    // BUG: Diagnostic contains: [ANNOTATIONS]
                    method.getDefaultValue();
                    // BUG: Diagnostic contains: [REFLECTIVE_ACCESS]
                    field.get(null);
                    // BUG: Diagnostic contains: [REFLECTIVE_ACCESS]
                    InstantiationUtils.instantiate(type);
                    // BUG: Diagnostic contains: [CLASS_LOADING]
                    ClassLoader.getSystemClassLoader().loadClass("x");
                    // BUG: Diagnostic contains: [CLASS_LOADING]
                    ClassUtils.forName("x", null);
                    // BUG: Diagnostic contains: [SERVICE_LOADING]
                    java.util.ServiceLoader.load(Runnable.class);
                    // BUG: Diagnostic contains: [SERVICE_LOADING]
                    provider.get();
                    // BUG: Diagnostic contains: [TARGET_MEMBERS]
                    point.getField();
                }
                // BUG: Diagnostic contains: [CLASS_NAMES]
                Function<Class<?>, String> names = Class::getSimpleName;
                // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                Function<Class<Colour>, EnumMap<Colour, String>> maps = EnumMap::new;
            }
            """).doTest();
    }

    /**
     * The ways a call is written: a static import, a method or constructor reference, the constructor of an anonymous
     * subclass or a super call, a call through a subtype or from a subclass, and wherever the code sits.
     */
    @Test
    void reportsACallHoweverItIsWritten() {
        helper().addSourceLines("Subject.java", """
            import static java.lang.Class.forName;
            import java.lang.invoke.MethodType;
            import java.lang.invoke.MutableCallSite;
            import java.util.EnumMap;
            import java.util.function.Function;
            import java.util.function.Supplier;
            class Subject {
                enum Colour { RED }
                // BUG: Diagnostic contains: [CLASS_MEMBERS]
                static final Object FIELDS = Subject.class.getDeclaredFields();
                // BUG: Diagnostic contains: [CLASS_MEMBERS]
                Supplier<Object> later = () -> Subject.class.getDeclaredMethods();
                static {
                    // BUG: Diagnostic contains: [CLASS_NAMES]
                    Subject.class.getSimpleName();
                }
                static class Colours extends EnumMap<Colour, String> {
                    Colours() {
                        // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                        super(Colour.class);
                    }
                }
                static class Loader extends ClassLoader {
                    Class<?> load(String name) throws ClassNotFoundException {
                        // BUG: Diagnostic contains: [CLASS_LOADING]
                        return loadClass(name);
                    }
                }
                // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                Function<Class<Colour>, EnumMap<Colour, String>> maps = EnumMap::new;
                void write(MethodType type, java.lang.reflect.Method method, Loader loader) throws Exception {
                    // BUG: Diagnostic contains: [CLASS_LOADING]
                    forName("java.lang.String");
                    // BUG: Diagnostic contains: [ENUM_CONSTANTS]
                    new EnumMap<Colour, String>(Colour.class) { };
                    // BUG: Diagnostic contains: [HANDLES]
                    new MutableCallSite(type) { };
                    // BUG: Diagnostic contains: [ANNOTATIONS]
                    method.getAnnotation(Deprecated.class);
                    // BUG: Diagnostic contains: [CLASS_LOADING]
                    loader.loadClass("java.lang.String");
                    Runnable local = new Runnable() {
                        public void run() {
                            // BUG: Diagnostic contains: [INTERFACES]
                            Subject.class.getInterfaces();
                        }
                    };
                }
            }
            """).doTest();
    }

    /** Every part of java.lang.invoke, with the bootstraps and resolution that lead to it, but its exceptions. */
    @Test
    void reportsAllOfJavaLangInvoke() {
        helper().addSourceLines("Subject.java", """
            import java.lang.constant.ClassDesc;
            import java.lang.invoke.*;
            class Subject {
                void handles(MethodHandles.Lookup lookup, MethodHandle handle, VarHandle variable, MethodHandleInfo info,
                             SerializedLambda lambda, MethodType type, ClassDesc description) throws Throwable {
                    // BUG: Diagnostic contains: [HANDLES]
                    MethodType.methodType(void.class);
                    // BUG: Diagnostic contains: [HANDLES]
                    new MutableCallSite(type);
                    // BUG: Diagnostic contains: [HANDLES]
                    handle.invokeExact();
                    // BUG: Diagnostic contains: [HANDLES]
                    variable.get();
                    // BUG: Diagnostic contains: [HANDLES]
                    info.reflectAs(java.lang.reflect.Method.class, lookup);
                    // BUG: Diagnostic contains: [HANDLES]
                    lambda.getImplMethodName();
                    // BUG: Diagnostic contains: [HANDLES]
                    LambdaMetafactory.metafactory(lookup, "run", type, type, handle, type);
                    // BUG: Diagnostic contains: [HANDLES]
                    StringConcatFactory.makeConcat(lookup, "concat", type);
                    // BUG: Diagnostic contains: [HANDLES]
                    ConstantBootstraps.nullConstant(lookup, "x", Object.class);
                    // BUG: Diagnostic contains: [HANDLES]
                    new SwitchPoint();
                    // BUG: Diagnostic contains: [HANDLES]
                    java.lang.runtime.ObjectMethods.bootstrap(lookup, "toString", type, Subject.class, "", new MethodHandle[0]);
                    // BUG: Diagnostic contains: [HANDLES]
                    description.resolveConstantDesc(lookup);
                    // BUG: Diagnostic contains: [PROXY]
                    MethodHandleProxies.asInterfaceInstance(Runnable.class, handle);
                    Object exception = new WrongMethodTypeException("an exception of the package is not reached for");
                    Object name = description.descriptorString();
                }
            }
            """).doTest();
    }

    /** A call of the same name on a type that is not reflective is not reported: what the call resolved to decides. */
    @Test
    void doesNotReportCallsOfTheSameNameOnOtherTypes() {
        helper().addSourceLines("Subject.java", """
            import io.micronaut.core.annotation.AnnotationMetadata;
            import io.micronaut.core.reflect.ClassUtils;
            import io.micronaut.core.reflect.ReflectionUtils;
            class Subject {
                enum Colour { RED }
                Object read(AnnotationMetadata metadata, Class<?> type) {
                    metadata.getAnnotation("Deprecated");
                    metadata.getDeclaredAnnotation("Deprecated");
                    ReflectionUtils.getWrapperType(int.class);
                    ClassUtils.isJavaLangType(type);
                    Colour.values();
                    String.valueOf("x");
                    Character.valueOf('x');
                    type.getName();
                    type.getSuperclass();
                    type.isEnum();
                    return type.getModifiers();
                }
            }
            """).doTest();
    }

    /** Allowed categories are not reported, and every other one still is. */
    @Test
    void allowsTheCategoriesTheBuildNames() {
        helper()
            .setArgs("-XepOpt:NoReflection:Allowed=ANNOTATION_SYNTHESIS,TARGET_MEMBERS")
            .addSourceLines("Subject.java", """
                import io.micronaut.core.annotation.AnnotationMetadata;
                import io.micronaut.inject.ExecutableMethod;
                class Subject {
                    void reflect(Class<?> type, AnnotationMetadata metadata, ExecutableMethod method) {
                        metadata.synthesize(Deprecated.class);
                        method.getTargetMethod();
                        // BUG: Diagnostic contains: [CLASS_MEMBERS]
                        type.getDeclaredMethods();
                    }
                }
                """).doTest();
    }

    /** A call the build allows is not reported, with a type named exactly, a type and its subtypes, or any member. */
    @Test
    void allowsTheCallsTheBuildNames() {
        helper()
            .setArgs("-XepOpt:NoReflection:AllowedCalls=java.lang.Class#getSimpleName,java.util.EnumSet#*,"
                + "java.lang.reflect.AnnotatedElement+#getAnnotation")
            .addSourceLines("Subject.java", """
                import java.util.EnumSet;
                class Subject {
                    enum Colour { RED }
                    void reflect(Class<?> type, java.lang.reflect.Method method) {
                        type.getSimpleName();
                        EnumSet.noneOf(Colour.class);
                        type.getAnnotation(Deprecated.class);
                        method.getAnnotation(Deprecated.class);
                        // BUG: Diagnostic contains: [CLASS_NAMES]
                        type.getCanonicalName();
                        // BUG: Diagnostic contains: [ANNOTATIONS]
                        type.getAnnotations();
                    }
                }
                """).doTest();
    }

    /** The calls a build forbids besides the categories are reported as CUSTOM, constructors of subtypes included. */
    @Test
    void reportsTheCallsTheBuildForbids() {
        helper()
            .setArgs("-XepOpt:NoReflection:ForbiddenCalls=example.Registry#lookup|find,example.Plugin+#<init>")
            .addSourceLines("example/Registry.java", """
                package example;
                public final class Registry {
                    public static Object lookup(String name) { return null; }
                    public static Object find(String name) { return null; }
                    public static Object other(String name) { return null; }
                }
                """)
            .addSourceLines("example/Plugin.java", """
                package example;
                public class Plugin {
                }
                """)
            .addSourceLines("example/Subject.java", """
                package example;
                class Subject {
                    static class LocalPlugin extends Plugin {
                    }
                    void call() {
                        // BUG: Diagnostic contains: [CUSTOM]
                        Registry.lookup("x");
                        // BUG: Diagnostic contains: [CUSTOM]
                        Registry.find("x");
                        Registry.other("x");
                        // BUG: Diagnostic contains: [CUSTOM]
                        new Plugin();
                        // BUG: Diagnostic contains: [CUSTOM]
                        new LocalPlugin();
                    }
                }
                """)
            .doTest();
    }

    /**
     * Reflection in a named class is allowed, with the classes and lambdas nested in it, whichever way a nested class
     * is named; the same call anywhere else is still reported.
     */
    @Test
    void allowsReflectionOnlyInTheNamedClasses() {
        helper()
            .setArgs("-XepOpt:NoReflection:AllowedIn=example.ReflectionAccess,example.Holder$Nested")
            .addSourceLines("example/ReflectionAccess.java", """
                package example;
                import java.util.function.Supplier;
                final class ReflectionAccess {
                    static Object members(Class<?> type) {
                        Supplier<Object> later = () -> type.getDeclaredFields();
                        return type.getDeclaredMethods();
                    }
                    static final class Inner {
                        Object field(Class<?> type) throws Exception {
                            return type.getDeclaredField("x");
                        }
                    }
                }
                """)
            .addSourceLines("example/Holder.java", """
                package example;
                class Holder {
                    Object outside(Class<?> type) {
                        // BUG: Diagnostic contains: [CLASS_MEMBERS]
                        return type.getDeclaredMethods();
                    }
                    static class Nested {
                        Object inside(Class<?> type) {
                            return type.getDeclaredMethods();
                        }
                    }
                }
                """)
            .addSourceLines("example/Elsewhere.java", """
                package example;
                class Elsewhere {
                    Object members(Class<?> type) {
                        // BUG: Diagnostic contains: [CLASS_MEMBERS]
                        return type.getDeclaredMethods();
                    }
                }
                """)
            .doTest();
    }

    /** A class can be allowed some categories only, and a package any, with the packages below it but no sibling. */
    @Test
    void allowsTheNamedCategoriesInAClassAndAnyInAPackage() {
        helper()
            .setArgs("-XepOpt:NoReflection:AllowedIn=example.Handles:HANDLES+PROXY,example.internal.*")
            .addSourceLines("example/Handles.java", """
                package example;
                final class Handles {
                    Object handles(Class<?> type) {
                        java.lang.invoke.MethodHandles.lookup();
                        // BUG: Diagnostic contains: [CLASS_MEMBERS]
                        return type.getDeclaredMethods();
                    }
                }
                """)
            .addSourceLines("example/internal/deep/Anything.java", """
                package example.internal.deep;
                final class Anything {
                    Object anything(Class<?> type) {
                        java.lang.invoke.MethodHandles.lookup();
                        return type.getDeclaredMethods();
                    }
                }
                """)
            .addSourceLines("example/internals/Sibling.java", """
                package example.internals;
                final class Sibling {
                    Object members(Class<?> type) {
                        // BUG: Diagnostic contains: [CLASS_MEMBERS]
                        return type.getDeclaredMethods();
                    }
                }
                """)
            .doTest();
    }

    /** A suppression on the variable allows that call, and nothing else in the method. */
    @Test
    void honoursASuppressionOnTheVariableAndNoFurther() {
        helper().addSourceLines("Subject.java", """
            class Subject {
                Object reflect(Class<?> type) {
                    @SuppressWarnings("NoReflection")
                    Object allowed = type.getDeclaredMethods();
                    // BUG: Diagnostic contains: [CLASS_MEMBERS]
                    type.getDeclaredFields();
                    return allowed;
                }
            }
            """).doTest();
    }

    /** A suppression on the method or on a class reaches every call inside it, and "all" suppresses the check too. */
    @Test
    void honoursASuppressionOnAnEnclosingDeclaration() {
        helper().addSourceLines("Subject.java", """
            class Subject {
                @SuppressWarnings("NoReflection")
                Object method(Class<?> type) {
                    return type.getDeclaredMethods();
                }
                @SuppressWarnings({"unchecked", "all"})
                static class Nested {
                    Object nested(Class<?> type) {
                        return type.getDeclaredFields();
                    }
                }
                Object unsuppressed(Class<?> type) {
                    // BUG: Diagnostic contains: [CLASS_MEMBERS]
                    return type.getDeclaredConstructors();
                }
            }
            """).doTest();
    }

    /** A build that honours no suppression still allows what it allows, and nothing a suppression in the source asks for. */
    @Test
    void honoursNoSuppressionWhenTheBuildSaysSo() {
        helper()
            .setArgs("-XepOpt:NoReflection:Suppressible=false", "-XepOpt:NoReflection:Allowed=CLASS_NAMES",
                "-XepOpt:NoReflection:AllowedIn=example.ReflectionAccess")
            .addSourceLines("example/ReflectionAccess.java", """
                package example;
                final class ReflectionAccess {
                    static Object members(Class<?> type) {
                        return type.getDeclaredMethods();
                    }
                }
                """)
            .addSourceLines("example/Elsewhere.java", """
                package example;
                @SuppressWarnings("NoReflection")
                class Elsewhere {
                    @SuppressWarnings("all")
                    Object members(Class<?> type) {
                        Object name = type.getSimpleName();
                        @SuppressWarnings("NoReflection")
                        // BUG: Diagnostic contains: [CLASS_MEMBERS]
                        Object members = type.getDeclaredMethods();
                        return members;
                    }
                }
                """)
            .doTest();
    }

    @Test
    void reportsEveryCategoryWhenNoSuppressionIsHonoured() {
        helper().setArgs("-XepOpt:NoReflection:Suppressible=false").addSourceLines("Subject.java", EVERY_CATEGORY).doTest();
    }
}
