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

import java.util.Arrays;
import java.util.Locale;

/**
 * The kinds of reflection the checks tell apart, which is what a project allows when it allows any.
 *
 * <p>The kinds follow what the platform does when it is called. Most fill a cache the virtual machine keeps for a class
 * or a member - the reflection data of a class (its members, names and interfaces), its enum constants, its generic
 * signature and annotations, the accessor of a method or a field - and the others load, define or proxy classes, or
 * reach members by name. A call is reported under the first category, in the order declared here, that it belongs to.</p>
 *
 * <p>The names are what the flags of the checks take and what the Gradle plugin's
 * {@code noReflection { allow(...) }} names. The flags are declared here rather than on the check so that the
 * plugin reads them without loading a class that needs ErrorProne.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
public enum ReflectionCategory {

    /** Building an annotation instance from Micronaut's annotation metadata, which defines a proxy class to do it. */
    ANNOTATION_SYNTHESIS,

    /** The {@code java.lang.reflect} member behind a Micronaut method or field: {@code getTargetMethod}, {@code FieldInjectionPoint.getField}. */
    TARGET_MEMBERS,

    /** Micronaut's {@code ReflectionUtils}, but for its table mapping primitive types to their wrappers and back. */
    REFLECTION_UTILS,

    /** {@code java.beans}: introspection, feature descriptors, statements, encoders and event handlers. */
    BEANS,

    /** Java serialization, which reads the fields, constructors and private methods of the classes it meets. */
    SERIALIZATION,

    /** {@code sun.misc.Unsafe} and {@code sun.reflect.ReflectionFactory}. */
    UNSAFE,

    /** {@code java.lang.instrument.Instrumentation}, which lists, redefines and retransforms loaded classes. */
    INSTRUMENTATION,

    /** Proxy classes: {@code java.lang.reflect.Proxy}, {@code MethodHandleProxies} and {@code InvocationHandler.invokeDefault}. */
    PROXY,

    /**
     * All of {@code java.lang.invoke} but its exceptions - method and variable handles, lookups, call sites, and the
     * factories and bootstraps behind lambdas and string concatenation - with the bootstraps of {@code java.lang.runtime}
     * for records and switches, the resolution of {@code java.lang.constant} descriptions, and the handles of
     * {@code java.lang.foreign.Linker}.
     */
    HANDLES,

    /** {@code java.util.ServiceLoader} and Micronaut's {@code SoftServiceLoader}, which instantiate what they find reflectively. */
    SERVICE_LOADING,

    /**
     * Classes loaded by name or defined at run time: {@code Class.forName}, a {@code ClassLoader}, module layers,
     * resource bundles and Micronaut's {@code ClassUtils.forName}.
     */
    CLASS_LOADING,

    /** {@code AtomicReferenceFieldUpdater}, {@code AtomicIntegerFieldUpdater} and {@code AtomicLongFieldUpdater}, which look their field up. */
    FIELD_UPDATERS,

    /**
     * The enum constants a class caches: {@code Class.getEnumConstants}, {@code Enum.valueOf} and the {@code valueOf(String)}
     * of every enum, which calls it, {@code EnumSet} and {@code EnumMap}.
     */
    ENUM_CONSTANTS,

    /** {@code Class.getSimpleName} and {@code Class.getCanonicalName}, which fill the reflection data of the class. */
    CLASS_NAMES,

    /** {@code Class.getInterfaces}, which fills the reflection data of the class, and Micronaut's {@code ClassUtils.resolveHierarchy}. */
    INTERFACES,

    /** Generic signatures, parsed and cached for a class or a member, the types they produce, and Micronaut's {@code GenericTypeUtils}. */
    GENERIC_SIGNATURES,

    /** Annotations read from a class, a member, a parameter or a type use, rather than from Micronaut's metadata. */
    ANNOTATIONS,

    /**
     * The members of a class looked up: its methods, constructors and fields, record components, nested and permitted
     * classes, nest members, enclosing method, and the parameters of a method.
     */
    CLASS_MEMBERS,

    /**
     * A member reached once it is found: {@code setAccessible}, invoking a method, reading or writing a field, creating
     * an instance reflectively, {@code Array}, opening a module, and Micronaut's {@code InstantiationUtils}.
     */
    REFLECTIVE_ACCESS,

    /** A call the build forbids besides the categories above, with {@code noReflection { forbid(...) }}. */
    CUSTOM;

    /** The flag naming the categories allowed throughout the project, separated by commas. */
    public static final String ALLOWED_FLAG = "NoReflection:Allowed";

    /** The flag naming the calls allowed throughout the project, as {@link CallPattern}s separated by commas. */
    public static final String ALLOWED_CALLS_FLAG = "NoReflection:AllowedCalls";

    /** The flag naming the classes and packages in which reflection is allowed, as {@link AllowedScope}s separated by commas. */
    public static final String ALLOWED_IN_FLAG = "NoReflection:AllowedIn";

    /** The flag naming the calls reported as {@link #CUSTOM}, as {@link CallPattern}s separated by commas. */
    public static final String FORBIDDEN_CALLS_FLAG = "NoReflection:ForbiddenCalls";

    /** The flag saying whether a {@code @SuppressWarnings("NoReflection")} in the source is honoured, unless it is {@code false}. */
    public static final String SUPPRESSIBLE_FLAG = "NoReflection:Suppressible";

    /**
     * @param name The name of a category, in any case
     * @return The category
     * @throws IllegalArgumentException When no category has the name, naming the ones there are
     */
    public static ReflectionCategory named(String name) {
        String normalized = name.strip().toUpperCase(Locale.ROOT);
        for (ReflectionCategory category : values()) {
            if (category.name().equals(normalized)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown reflection category '" + name + "'. The categories are "
            + Arrays.toString(values()) + ".");
    }
}
