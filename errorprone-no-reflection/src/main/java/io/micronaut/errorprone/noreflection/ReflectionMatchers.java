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

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What each {@link ReflectionProblem}, and so each {@link ReflectionCategory}, matches, written as {@link CallPattern}s.
 *
 * <p>A call is matched by the method the compiler resolved it to, which is what tells a reflective call from one of the
 * same name that is not: {@code Class.getAnnotation} asks the platform, {@code AnnotationMetadata.getAnnotation} reads
 * what Micronaut compiled, and only the first is matched. The categories are tried in the order they are declared, so a
 * call two of them name is reported under the first - {@code Field.getGenericType} as a generic signature rather than
 * as access to a field.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
final class ReflectionMatchers {

    private static final List<Rule> RULES = new ArrayList<>();

    static {
        rules(ReflectionProblem.ANNOTATION_SYNTHESIS,
            "io.micronaut.core.annotation.AnnotationSource+#synthesize|synthesizeDeclared|synthesizeAll"
                + "|synthesizeAnnotationsByType|synthesizeDeclaredAnnotationsByType");
        rules(ReflectionProblem.TARGET_METHOD,
            "io.micronaut.inject.MethodReference+#getTargetMethod");
        rules(ReflectionProblem.INJECTION_POINT_FIELD,
            "io.micronaut.inject.FieldInjectionPoint+#getField");
        // any method of the helper but the ones that read a table compiled into the class, check a name or build an
        // error message; matching the class keeps a helper added later inside
        rules(ReflectionProblem.REFLECTION_UTILS,
            "io.micronaut.core.reflect.ReflectionUtils#*-getWrapperType|getPrimitiveType|isSetter|newNoSuchMethodError");
        rules(ReflectionProblem.BEAN_INFO,
            "java.beans.Introspector#getBeanInfo");
        rules(ReflectionProblem.BEANS_INSTANTIATE,
            "java.beans.Beans#instantiate|isInstanceOf|getInstanceOf");
        rules(ReflectionProblem.BEAN_STATEMENT,
            "java.beans.Statement+#<init>|execute|getValue");
        rules(ReflectionProblem.FEATURE_DESCRIPTOR,
            "java.beans.FeatureDescriptor+#<init>");
        rules(ReflectionProblem.PROPERTY_DESCRIPTOR,
            "java.beans.PropertyDescriptor+#getPropertyType|getReadMethod|setReadMethod|getWriteMethod|setWriteMethod"
                + "|createPropertyEditor|getIndexedPropertyType|getIndexedReadMethod|setIndexedReadMethod"
                + "|getIndexedWriteMethod|setIndexedWriteMethod");
        rules(ReflectionProblem.METHOD_DESCRIPTOR,
            "java.beans.MethodDescriptor#getMethod");
        rules(ReflectionProblem.EVENT_SET_DESCRIPTOR,
            "java.beans.EventSetDescriptor#getAddListenerMethod|getRemoveListenerMethod|getGetListenerMethod|getListenerMethods");
        rules(ReflectionProblem.EVENT_HANDLER,
            "java.beans.EventHandler#create|invoke");
        rules(ReflectionProblem.BEAN_ENCODER,
            "java.beans.Encoder+#writeObject|writeStatement|writeExpression|getPersistenceDelegate");
        rules(ReflectionProblem.PERSISTENCE_DELEGATE,
            "java.beans.PersistenceDelegate+#writeObject|instantiate|initialize");
        rules(ReflectionProblem.XML_DECODER,
            "java.beans.XMLDecoder#readObject");
        rules(ReflectionProblem.PROPERTY_EDITOR,
            "java.beans.PropertyEditorManager#findEditor");
        rules(ReflectionProblem.OBJECT_INPUT,
            "java.io.ObjectInputStream+#readObject|readUnshared|defaultReadObject|readFields|resolveClass|resolveProxyClass");
        rules(ReflectionProblem.OBJECT_OUTPUT,
            "java.io.ObjectOutputStream+#writeObject|writeUnshared|defaultWriteObject|putFields|writeFields");
        rules(ReflectionProblem.OBJECT_STREAM_CLASS,
            "java.io.ObjectStreamClass#lookup|lookupAny|forClass|getFields|getField|getSerialVersionUID");
        rules(ReflectionProblem.UNSAFE,
            "sun.misc.Unsafe#*");
        rules(ReflectionProblem.REFLECTION_FACTORY,
            "sun.reflect.ReflectionFactory#*");
        rules(ReflectionProblem.INSTRUMENTATION,
            "java.lang.instrument.Instrumentation+#*");
        rules(ReflectionProblem.PROXY,
            "java.lang.reflect.Proxy#*");
        rules(ReflectionProblem.INVOKE_DEFAULT,
            "java.lang.reflect.InvocationHandler#invokeDefault");
        rules(ReflectionProblem.METHOD_HANDLE_PROXIES,
            "java.lang.invoke.MethodHandleProxies#*");
        rules(ReflectionProblem.METHOD_HANDLES,
            "java.lang.invoke.*#*");
        rules(ReflectionProblem.OBJECT_METHODS,
            "java.lang.runtime.ObjectMethods#*");
        rules(ReflectionProblem.SWITCH_BOOTSTRAPS,
            "java.lang.runtime.SwitchBootstraps#*");
        rules(ReflectionProblem.CONSTANT_RESOLUTION,
            "java.lang.constant.ConstantDesc+#resolveConstantDesc",
            "java.lang.constant.DynamicCallSiteDesc#resolveCallSiteDesc");
        rules(ReflectionProblem.FOREIGN_LINKER,
            "java.lang.foreign.Linker+#downcallHandle|upcallStub");
        rules(ReflectionProblem.SERVICE_LOADER,
            "java.util.ServiceLoader#*");
        rules(ReflectionProblem.SERVICE_PROVIDER,
            "java.util.ServiceLoader.Provider+#get");
        rules(ReflectionProblem.SOFT_SERVICE_LOADER,
            "io.micronaut.core.io.service.SoftServiceLoader#*");
        rules(ReflectionProblem.CLASS_FOR_NAME,
            "java.lang.Class#forName");
        rules(ReflectionProblem.CLASS_LOADER,
            "java.lang.ClassLoader+#loadClass|findClass|findLoadedClass|findSystemClass|defineClass|resolveClass");
        rules(ReflectionProblem.MODULE_LAYER,
            "java.lang.ModuleLayer#defineModules|defineModulesWithOneLoader|defineModulesWithManyLoaders");
        rules(ReflectionProblem.RESOURCE_BUNDLE,
            "java.util.ResourceBundle#getBundle",
            "java.util.ResourceBundle.Control+#newBundle");
        rules(ReflectionProblem.CLASS_UTILS_FOR_NAME,
            "io.micronaut.core.reflect.ClassUtils#forName|isPresent");
        rules(ReflectionProblem.FIELD_UPDATER,
            "java.util.concurrent.atomic.AtomicReferenceFieldUpdater#newUpdater",
            "java.util.concurrent.atomic.AtomicIntegerFieldUpdater#newUpdater",
            "java.util.concurrent.atomic.AtomicLongFieldUpdater#newUpdater");
        rules(ReflectionProblem.ENUM_CONSTANTS,
            "java.lang.Class#getEnumConstants");
        rules(ReflectionProblem.ENUM_VALUE_OF,
            "java.lang.Enum#valueOf");
        rules(ReflectionProblem.ENUM_DESCRIPTION,
            "java.lang.Enum#describeConstable",
            "java.lang.Enum.EnumDesc#of");
        rules(ReflectionProblem.ENUM_SET,
            "java.util.EnumSet#noneOf|allOf|of|range|copyOf");
        rules(ReflectionProblem.ENUM_MAP,
            "java.util.EnumMap#<init>");
        rules(ReflectionProblem.SIMPLE_NAME,
            "java.lang.Class#getSimpleName");
        rules(ReflectionProblem.CANONICAL_NAME,
            "java.lang.Class#getCanonicalName");
        rules(ReflectionProblem.CLASS_INTERFACES,
            "java.lang.Class#getInterfaces");
        rules(ReflectionProblem.CLASS_HIERARCHY,
            "io.micronaut.core.reflect.ClassUtils#resolveHierarchy");
        rules(ReflectionProblem.CLASS_GENERIC_SIGNATURE,
            "java.lang.Class#getGenericSuperclass|getGenericInterfaces|getTypeParameters|toGenericString");
        rules(ReflectionProblem.MEMBER_GENERIC_SIGNATURE,
            "java.lang.reflect.Executable+#getGenericParameterTypes|getGenericExceptionTypes|getGenericReturnType"
                + "|getTypeParameters|toGenericString",
            "java.lang.reflect.Field#getGenericType|toGenericString",
            "java.lang.reflect.RecordComponent#getGenericType|getGenericSignature");
        rules(ReflectionProblem.PARAMETERIZED_TYPE,
            "java.lang.reflect.Parameter#getParameterizedType");
        rules(ReflectionProblem.GENERIC_TYPES,
            "java.lang.reflect.ParameterizedType+#*",
            "java.lang.reflect.TypeVariable+#*",
            "java.lang.reflect.WildcardType+#*",
            "java.lang.reflect.GenericArrayType+#*");
        rules(ReflectionProblem.GENERIC_TYPE_UTILS,
            "io.micronaut.core.reflect.GenericTypeUtils#*");
        rules(ReflectionProblem.DECLARED_ANNOTATIONS,
            "java.lang.reflect.AnnotatedElement+#getAnnotation|getAnnotations|getDeclaredAnnotation|getDeclaredAnnotations"
                + "|getAnnotationsByType|getDeclaredAnnotationsByType|isAnnotationPresent");
        rules(ReflectionProblem.PARAMETER_ANNOTATIONS,
            "java.lang.reflect.Executable+#getParameterAnnotations");
        rules(ReflectionProblem.ANNOTATION_DEFAULT,
            "java.lang.reflect.Method#getDefaultValue");
        rules(ReflectionProblem.ANNOTATED_TYPES,
            "java.lang.Class#getAnnotatedSuperclass|getAnnotatedInterfaces",
            "java.lang.reflect.Executable+#getAnnotatedReturnType|getAnnotatedReceiverType|getAnnotatedParameterTypes"
                + "|getAnnotatedExceptionTypes",
            "java.lang.reflect.Field#getAnnotatedType",
            "java.lang.reflect.RecordComponent#getAnnotatedType",
            "java.lang.reflect.Parameter#getAnnotatedType",
            "java.lang.reflect.AnnotatedType+#*");
        rules(ReflectionProblem.PUBLIC_METHODS,
            "java.lang.Class#getMethod|getMethods");
        rules(ReflectionProblem.DECLARED_METHODS,
            "java.lang.Class#getDeclaredMethod|getDeclaredMethods");
        rules(ReflectionProblem.PUBLIC_CONSTRUCTORS,
            "java.lang.Class#getConstructor|getConstructors");
        rules(ReflectionProblem.DECLARED_CONSTRUCTORS,
            "java.lang.Class#getDeclaredConstructor|getDeclaredConstructors");
        rules(ReflectionProblem.PUBLIC_FIELDS,
            "java.lang.Class#getField|getFields");
        rules(ReflectionProblem.DECLARED_FIELDS,
            "java.lang.Class#getDeclaredField|getDeclaredFields");
        rules(ReflectionProblem.RECORD_COMPONENTS,
            "java.lang.Class#getRecordComponents",
            "java.lang.reflect.RecordComponent#getAccessor");
        rules(ReflectionProblem.PERMITTED_SUBCLASSES,
            "java.lang.Class#getPermittedSubclasses");
        rules(ReflectionProblem.NEST_MEMBERS,
            "java.lang.Class#getNestMembers");
        rules(ReflectionProblem.NESTED_CLASSES,
            "java.lang.Class#getClasses|getDeclaredClasses");
        rules(ReflectionProblem.ENCLOSING_MEMBER,
            "java.lang.Class#getEnclosingMethod|getEnclosingConstructor");
        rules(ReflectionProblem.EXECUTABLE_PARAMETERS,
            "java.lang.reflect.Executable+#getParameters");
        rules(ReflectionProblem.ACCESSIBILITY,
            "java.lang.reflect.AccessibleObject+#setAccessible|trySetAccessible|canAccess|isAccessible");
        rules(ReflectionProblem.CONSTRUCTOR_ACCESSOR,
            "java.lang.reflect.Constructor#newInstance");
        rules(ReflectionProblem.METHOD_ACCESSOR,
            "java.lang.reflect.Method#invoke");
        // what reads or writes the value of a field, which makes its accessor; its name, type and modifiers do not
        rules(ReflectionProblem.FIELD_ACCESSOR,
            "java.lang.reflect.Field#get|getBoolean|getByte|getChar|getShort|getInt|getLong|getFloat|getDouble"
                + "|set|setBoolean|setByte|setChar|setShort|setInt|setLong|setFloat|setDouble");
        rules(ReflectionProblem.ARRAYS,
            "java.lang.reflect.Array#*");
        rules(ReflectionProblem.CLASS_NEW_INSTANCE,
            "java.lang.Class#newInstance");
        rules(ReflectionProblem.OPEN_MODULE,
            "java.lang.Module#addOpens",
            "java.lang.ModuleLayer.Controller#addOpens");
        rules(ReflectionProblem.INSTANTIATION_UTILS,
            "io.micronaut.core.reflect.InstantiationUtils#*");

        Set<ReflectionProblem> uncovered = EnumSet.allOf(ReflectionProblem.class);
        uncovered.remove(ReflectionProblem.CUSTOM);
        RULES.forEach(rule -> uncovered.remove(rule.problem()));
        if (!uncovered.isEmpty()) {
            throw new IllegalStateException("No calls for the reflection problems " + uncovered);
        }
    }

    private ReflectionMatchers() {
    }

    private static void rules(ReflectionProblem problem, String... patterns) {
        for (String pattern : patterns) {
            RULES.add(new Rule(problem, CallPattern.parse(pattern)));
        }
    }

    /**
     * @return The calls of each category as the patterns they are written as, in the order they are tried
     */
    static Map<ReflectionCategory, List<String>> patterns() {
        Map<ReflectionCategory, List<String>> patterns = new EnumMap<>(ReflectionCategory.class);
        for (Rule rule : RULES) {
            patterns.computeIfAbsent(rule.problem().category(), category -> new ArrayList<>()).add(rule.pattern().toString());
        }
        return patterns;
    }

    /**
     * @return The calls of each problem as the patterns they are written as, in the order they are tried
     */
    static Map<ReflectionProblem, List<String>> problemPatterns() {
        Map<ReflectionProblem, List<String>> patterns = new EnumMap<>(ReflectionProblem.class);
        for (Rule rule : RULES) {
            patterns.computeIfAbsent(rule.problem(), problem -> new ArrayList<>()).add(rule.pattern().toString());
        }
        return patterns;
    }

    /**
     * @param method The method, or constructor, a call or a reference resolved to
     * @param state  The state
     * @return The problem the method belongs to, or {@code null} when it belongs to none
     */
    static ReflectionProblem problemOf(Symbol.MethodSymbol method, VisitorState state) {
        for (Rule rule : RULES) {
            if (matches(rule.pattern(), method, state)) {
                return rule.problem();
            }
        }
        return isValueOfAnEnum(method) ? ReflectionProblem.ENUM_VALUE_OF : null;
    }

    /**
     * @param pattern A pattern
     * @param method  The method, or constructor, a call or a reference resolved to
     * @param state   The state
     * @return Whether the pattern names the method
     */
    static boolean matches(CallPattern pattern, Symbol.MethodSymbol method, VisitorState state) {
        if (!pattern.matchesMember(method.getSimpleName().toString())) {
            return false;
        }
        Symbol.ClassSymbol owner = method.enclClass();
        if (owner == null) {
            return false;
        }
        // an anonymous class is created through the constructor of the class it extends, which its body cannot hide
        if (method.isConstructor() && owner.isAnonymous() && owner.getSuperclass().tsym instanceof Symbol.ClassSymbol superclass) {
            owner = superclass;
        }
        return switch (pattern.ownerMatch()) {
            case EXACT -> names(pattern, owner);
            // the exceptions of a package are thrown, not reached for
            case PACKAGE -> pattern.coversPackage(owner.packge().getQualifiedName().toString())
                && !ASTHelpers.isSubtype(owner.type, state.getSymtab().throwableType, state);
            case DESCENDANTS -> {
                for (Type supertype : state.getTypes().closure(owner.type)) {
                    if (supertype.tsym instanceof Symbol.ClassSymbol type && names(pattern, type)) {
                        yield true;
                    }
                }
                yield false;
            }
        };
    }

    private static boolean names(CallPattern pattern, Symbol.ClassSymbol type) {
        return pattern.namesType(type.getQualifiedName().toString(), type.flatName().toString());
    }

    // the valueOf(String) the compiler writes for every enum calls Enum.valueOf, which fills the constants of the class
    private static boolean isValueOfAnEnum(Symbol.MethodSymbol method) {
        Symbol.ClassSymbol owner = method.enclClass();
        return owner != null && (owner.flags() & Flags.ENUM) != 0 && method.isStatic()
            && method.getSimpleName().contentEquals("valueOf") && method.type.getParameterTypes().size() == 1
            && method.type.getParameterTypes().head.tsym.getQualifiedName().contentEquals("java.lang.String");
    }

    private record Rule(ReflectionProblem problem, CallPattern pattern) {
    }
}
