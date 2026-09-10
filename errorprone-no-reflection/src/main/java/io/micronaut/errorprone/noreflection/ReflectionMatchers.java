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
 * What each {@link ReflectionCategory} matches, written as {@link CallPattern}s.
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
        rules(ReflectionCategory.ANNOTATION_SYNTHESIS,
            "io.micronaut.core.annotation.AnnotationSource+#synthesize|synthesizeDeclared|synthesizeAll"
                + "|synthesizeAnnotationsByType|synthesizeDeclaredAnnotationsByType");
        rules(ReflectionCategory.TARGET_MEMBERS,
            "io.micronaut.inject.MethodReference+#getTargetMethod",
            "io.micronaut.inject.FieldInjectionPoint+#getField");
        // any method of the helper but the ones that read a table compiled into the class, check a name or build an
        // error message; matching the class keeps a helper added later inside
        rules(ReflectionCategory.REFLECTION_UTILS,
            "io.micronaut.core.reflect.ReflectionUtils#*-getWrapperType|getPrimitiveType|isSetter|newNoSuchMethodError");
        rules(ReflectionCategory.BEANS,
            "java.beans.Introspector#getBeanInfo",
            "java.beans.Beans#instantiate|isInstanceOf|getInstanceOf",
            "java.beans.Statement+#<init>|execute|getValue",
            "java.beans.FeatureDescriptor+#<init>",
            "java.beans.PropertyDescriptor+#getPropertyType|getReadMethod|setReadMethod|getWriteMethod|setWriteMethod"
                + "|createPropertyEditor|getIndexedPropertyType|getIndexedReadMethod|setIndexedReadMethod"
                + "|getIndexedWriteMethod|setIndexedWriteMethod",
            "java.beans.MethodDescriptor#getMethod",
            "java.beans.EventSetDescriptor#getAddListenerMethod|getRemoveListenerMethod|getGetListenerMethod|getListenerMethods",
            "java.beans.EventHandler#create|invoke",
            "java.beans.Encoder+#writeObject|writeStatement|writeExpression|getPersistenceDelegate",
            "java.beans.PersistenceDelegate+#writeObject|instantiate|initialize",
            "java.beans.XMLDecoder#readObject",
            "java.beans.PropertyEditorManager#findEditor");
        rules(ReflectionCategory.SERIALIZATION,
            "java.io.ObjectInputStream+#readObject|readUnshared|defaultReadObject|readFields|resolveClass|resolveProxyClass",
            "java.io.ObjectOutputStream+#writeObject|writeUnshared|defaultWriteObject|putFields|writeFields",
            "java.io.ObjectStreamClass#lookup|lookupAny|forClass|getFields|getField|getSerialVersionUID");
        rules(ReflectionCategory.UNSAFE,
            "sun.misc.Unsafe#*",
            "sun.reflect.ReflectionFactory#*");
        rules(ReflectionCategory.INSTRUMENTATION,
            "java.lang.instrument.Instrumentation+#*");
        rules(ReflectionCategory.PROXY,
            "java.lang.reflect.Proxy#*",
            "java.lang.reflect.InvocationHandler#invokeDefault",
            "java.lang.invoke.MethodHandleProxies#*");
        rules(ReflectionCategory.HANDLES,
            "java.lang.invoke.*#*",
            "java.lang.runtime.ObjectMethods#*",
            "java.lang.runtime.SwitchBootstraps#*",
            "java.lang.constant.ConstantDesc+#resolveConstantDesc",
            "java.lang.constant.DynamicCallSiteDesc#resolveCallSiteDesc",
            "java.lang.foreign.Linker+#downcallHandle|upcallStub");
        rules(ReflectionCategory.SERVICE_LOADING,
            "java.util.ServiceLoader#*",
            "java.util.ServiceLoader.Provider+#get",
            "io.micronaut.core.io.service.SoftServiceLoader#*");
        rules(ReflectionCategory.CLASS_LOADING,
            "java.lang.Class#forName",
            "java.lang.ClassLoader+#loadClass|findClass|findLoadedClass|findSystemClass|defineClass|resolveClass",
            "java.lang.ModuleLayer#defineModules|defineModulesWithOneLoader|defineModulesWithManyLoaders",
            "java.util.ResourceBundle#getBundle",
            "java.util.ResourceBundle.Control+#newBundle",
            "io.micronaut.core.reflect.ClassUtils#forName|isPresent");
        rules(ReflectionCategory.FIELD_UPDATERS,
            "java.util.concurrent.atomic.AtomicReferenceFieldUpdater#newUpdater",
            "java.util.concurrent.atomic.AtomicIntegerFieldUpdater#newUpdater",
            "java.util.concurrent.atomic.AtomicLongFieldUpdater#newUpdater");
        rules(ReflectionCategory.ENUM_CONSTANTS,
            "java.lang.Class#getEnumConstants",
            "java.lang.Enum#valueOf|describeConstable",
            "java.lang.Enum.EnumDesc#of",
            "java.util.EnumSet#noneOf|allOf|of|range|copyOf",
            "java.util.EnumMap#<init>");
        rules(ReflectionCategory.CLASS_NAMES,
            "java.lang.Class#getSimpleName|getCanonicalName");
        rules(ReflectionCategory.INTERFACES,
            "java.lang.Class#getInterfaces",
            "io.micronaut.core.reflect.ClassUtils#resolveHierarchy");
        rules(ReflectionCategory.GENERIC_SIGNATURES,
            "java.lang.Class#getGenericSuperclass|getGenericInterfaces|getTypeParameters|toGenericString",
            "java.lang.reflect.Executable+#getGenericParameterTypes|getGenericExceptionTypes|getGenericReturnType"
                + "|getTypeParameters|toGenericString",
            "java.lang.reflect.Field#getGenericType|toGenericString",
            "java.lang.reflect.RecordComponent#getGenericType|getGenericSignature",
            "java.lang.reflect.Parameter#getParameterizedType",
            "java.lang.reflect.ParameterizedType+#*",
            "java.lang.reflect.TypeVariable+#*",
            "java.lang.reflect.WildcardType+#*",
            "java.lang.reflect.GenericArrayType+#*",
            "io.micronaut.core.reflect.GenericTypeUtils#*");
        rules(ReflectionCategory.ANNOTATIONS,
            "java.lang.reflect.AnnotatedElement+#getAnnotation|getAnnotations|getDeclaredAnnotation|getDeclaredAnnotations"
                + "|getAnnotationsByType|getDeclaredAnnotationsByType|isAnnotationPresent",
            "java.lang.Class#getAnnotatedSuperclass|getAnnotatedInterfaces",
            "java.lang.reflect.Executable+#getParameterAnnotations|getAnnotatedReturnType|getAnnotatedReceiverType"
                + "|getAnnotatedParameterTypes|getAnnotatedExceptionTypes",
            "java.lang.reflect.Method#getDefaultValue",
            "java.lang.reflect.Field#getAnnotatedType",
            "java.lang.reflect.RecordComponent#getAnnotatedType",
            "java.lang.reflect.Parameter#getAnnotatedType",
            "java.lang.reflect.AnnotatedType+#*");
        rules(ReflectionCategory.CLASS_MEMBERS,
            "java.lang.Class#getMethod|getMethods|getDeclaredMethod|getDeclaredMethods|getConstructor|getConstructors"
                + "|getDeclaredConstructor|getDeclaredConstructors|getField|getFields|getDeclaredField|getDeclaredFields"
                + "|getRecordComponents|getPermittedSubclasses|getNestMembers|getClasses|getDeclaredClasses"
                + "|getEnclosingMethod|getEnclosingConstructor",
            "java.lang.reflect.RecordComponent#getAccessor",
            "java.lang.reflect.Executable+#getParameters");
        rules(ReflectionCategory.REFLECTIVE_ACCESS,
            "java.lang.reflect.AccessibleObject+#setAccessible|trySetAccessible|canAccess|isAccessible",
            "java.lang.reflect.Constructor#newInstance",
            "java.lang.reflect.Method#invoke",
            // what reads or writes the value of a field, which makes its accessor; its name, type and modifiers do not
            "java.lang.reflect.Field#get|getBoolean|getByte|getChar|getShort|getInt|getLong|getFloat|getDouble"
                + "|set|setBoolean|setByte|setChar|setShort|setInt|setLong|setFloat|setDouble",
            "java.lang.reflect.Array#*",
            "java.lang.Class#newInstance",
            "java.lang.Module#addOpens",
            "java.lang.ModuleLayer.Controller#addOpens",
            "io.micronaut.core.reflect.InstantiationUtils#*");

        Set<ReflectionCategory> uncovered = EnumSet.allOf(ReflectionCategory.class);
        uncovered.remove(ReflectionCategory.CUSTOM);
        RULES.forEach(rule -> uncovered.remove(rule.category()));
        if (!uncovered.isEmpty()) {
            throw new IllegalStateException("No calls for the reflection categories " + uncovered);
        }
    }

    private ReflectionMatchers() {
    }

    private static void rules(ReflectionCategory category, String... patterns) {
        for (String pattern : patterns) {
            RULES.add(new Rule(category, CallPattern.parse(pattern)));
        }
    }

    /**
     * @return The calls of each category as the patterns they are written as, in the order they are tried
     */
    static Map<ReflectionCategory, List<String>> patterns() {
        Map<ReflectionCategory, List<String>> patterns = new EnumMap<>(ReflectionCategory.class);
        for (Rule rule : RULES) {
            patterns.computeIfAbsent(rule.category(), category -> new ArrayList<>()).add(rule.pattern().toString());
        }
        return patterns;
    }

    /**
     * @param method The method, or constructor, a call or a reference resolved to
     * @param state  The state
     * @return The category the method belongs to, or {@code null} when it belongs to none
     */
    static ReflectionCategory categoryOf(Symbol.MethodSymbol method, VisitorState state) {
        for (Rule rule : RULES) {
            if (matches(rule.pattern(), method, state)) {
                return rule.category();
            }
        }
        return isValueOfAnEnum(method) ? ReflectionCategory.ENUM_CONSTANTS : null;
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

    private record Rule(ReflectionCategory category, CallPattern pattern) {
    }
}
