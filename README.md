<!-- Checklist: https://github.com/micronaut-projects/micronaut-core/wiki/New-Module-Checklist -->

# ErrorProne No Reflection

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.errorprone/micronaut-errorprone-no-reflection.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.errorprone%22%20AND%20a:%22micronaut-errorprone-no-reflection%22)
[![Build Status](https://github.com/micronaut-projects/errorprone-no-reflection/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/errorprone-no-reflection/actions)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

An [ErrorProne](https://errorprone.info) check, `NoReflection`, that reports reflection: a call, a method reference or a constructor that reaches for it, named by the category it belongs to - most of them a cache the virtual machine fills for a class or a member. A project can allow categories, calls, classes and packages, and choose whether a suppression is honoured.

```kotlin
// settings.gradle.kts: the plugin is published to Maven Central rather than to the Gradle Plugin Portal
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    java
    id("io.micronaut.errorprone.no-reflection") version "1.0.0"
}

noReflection {
    allow("ENUM_CONSTANTS")
    allowIn("com.example.ReflectionAccess")
}
```

The check can also be added to ErrorProne on its own, as `errorprone("io.micronaut.errorprone:micronaut-errorprone-no-reflection:1.0.0")`, and configured with `-XepOpt:NoReflection:*` flags.

## Why

Micronaut works out at compile time what other frameworks look up by reflection at run time, and code built on it can do without reflection altogether. Reflection costs even when it is reached for once: the virtual machine creates and keeps, for every class it is asked about, the reflection data of its members, names and interfaces, its enum constants, generic signature and annotations, and the accessors of the members that are called; it loads classes by name and defines proxy classes; and a native image needs reachability metadata for every one of them.

This project makes "no reflection" something the compiler enforces rather than a convention to keep. It aims to:

* **Report every call whose purpose is reflection**, including the ones that do not look like it - `Class.getSimpleName`, `Enum.valueOf`, `EnumSet.noneOf` and the `valueOf(String)` of every enum fill a cache as surely as `getDeclaredMethods` does - and all of `java.lang.invoke`.
* **Say what kind of reflection it is**, so that a report tells what the call costs and a build can allow one kind without allowing the others.
* **Let a codebase get there gradually**: allow the categories it cannot do without yet, confine what remains to the classes or packages that need it, and report as warnings before failing the build.
* **Keep exceptions honest**: a suppression belongs on the variable holding what the platform returned, and a build can refuse suppressions altogether.

## What is reported

The check reports a call, a method reference or a constructor when the method or constructor it resolves to is among the calls below, and names the category it belongs to. The categories are tried in this order, and a call two of them name is reported under the first. The Micronaut categories, and the Micronaut types named in the others, match nothing in a project without Micronaut. The [guide](https://micronaut-projects.github.io/errorprone-no-reflection/latest/guide/#categories) lists the same calls with the pattern each is written as.

### `ANNOTATION_SYNTHESIS`

Micronaut keeps the values of annotations as metadata compiled into the application. Synthesizing turns them back into an instance of the annotation interface, which defines a proxy class for it at run time and needs registering for a native image. Reading the metadata itself - `getAnnotation`, `stringValue` and the like - is not reported.

```java
Deprecated deprecated = metadata.synthesize(Deprecated.class);
```

- `io.micronaut.core.annotation.AnnotationSource` and every subtype of it: `synthesize`, `synthesizeDeclared`, `synthesizeAll`, `synthesizeAnnotationsByType`, `synthesizeDeclaredAnnotationsByType`

### `TARGET_MEMBERS`

Micronaut's executable methods and injection points describe members without reflection. Asking one for its `java.lang.reflect` member looks that member up, filling the reflection data of its class.

```java
Method method = executableMethod.getTargetMethod();
```

- `io.micronaut.inject.MethodReference` and every subtype of it: `getTargetMethod`
- `io.micronaut.inject.FieldInjectionPoint` and every subtype of it: `getField`

### `REFLECTION_UTILS`

Micronaut's helpers look members up, read and write fields and invoke methods reflectively. Left out are the ones that reach for nothing: the table that maps primitive types to their wrappers and back, the check of a setter's name, and the building of an error message.

```java
Method method = ReflectionUtils.getRequiredMethod(type, "name");
```

- `io.micronaut.core.reflect.ReflectionUtils`: every method but `getWrapperType`, `getPrimitiveType`, `isSetter`, `newNoSuchMethodError`

### `BEANS`

The JavaBeans introspector reads every public method of a class to find its properties, and descriptors, statements, encoders and event handlers look methods up and invoke them by name. What only handles names or caches, such as `Introspector.decapitalize`, is not reported.

```java
BeanInfo info = Introspector.getBeanInfo(type);
```

- `java.beans.Introspector`: `getBeanInfo`
- `java.beans.Beans`: `instantiate`, `isInstanceOf`, `getInstanceOf`
- `java.beans.Statement` and every subtype of it: its constructors, `execute`, `getValue`
- `java.beans.FeatureDescriptor` and every subtype of it: its constructors
- `java.beans.PropertyDescriptor` and every subtype of it: `getPropertyType`, `getReadMethod`, `setReadMethod`, `getWriteMethod`, `setWriteMethod`, `createPropertyEditor`, `getIndexedPropertyType`, `getIndexedReadMethod`, `setIndexedReadMethod`, `getIndexedWriteMethod`, `setIndexedWriteMethod`
- `java.beans.MethodDescriptor`: `getMethod`
- `java.beans.EventSetDescriptor`: `getAddListenerMethod`, `getRemoveListenerMethod`, `getGetListenerMethod`, `getListenerMethods`
- `java.beans.EventHandler`: `create`, `invoke`
- `java.beans.Encoder` and every subtype of it: `writeObject`, `writeStatement`, `writeExpression`, `getPersistenceDelegate`
- `java.beans.PersistenceDelegate` and every subtype of it: `writeObject`, `instantiate`, `initialize`
- `java.beans.XMLDecoder`: `readObject`
- `java.beans.PropertyEditorManager`: `findEditor`

### `SERIALIZATION`

Java serialization reads the fields, constructors and private `readObject` and `writeObject` methods of every class it meets, loads the classes a stream names, and needs serialization metadata in a native image.

```java
Object value = objectInputStream.readObject();
```

- `java.io.ObjectInputStream` and every subtype of it: `readObject`, `readUnshared`, `defaultReadObject`, `readFields`, `resolveClass`, `resolveProxyClass`
- `java.io.ObjectOutputStream` and every subtype of it: `writeObject`, `writeUnshared`, `defaultWriteObject`, `putFields`, `writeFields`
- `java.io.ObjectStreamClass`: `lookup`, `lookupAny`, `forClass`, `getFields`, `getField`, `getSerialVersionUID`

### `UNSAFE`

`sun.misc.Unsafe` reaches fields by their offset and allocates instances without calling a constructor; `sun.reflect.ReflectionFactory` makes the constructors serialization uses.

```java
Object instance = unsafe.allocateInstance(type);
```

- `sun.misc.Unsafe`: every method and constructor
- `sun.reflect.ReflectionFactory`: every method and constructor

### `INSTRUMENTATION`

An agent's `Instrumentation` lists, redefines and retransforms the loaded classes.

```java
Class<?>[] loaded = instrumentation.getAllLoadedClasses();
```

- `java.lang.instrument.Instrumentation` and every subtype of it: every method and constructor

### `PROXY`

A `java.lang.reflect.Proxy` class is defined at run time and lives as long as its class loader; `MethodHandleProxies` defines a hidden class for an interface; `InvocationHandler.invokeDefault` invokes a default method reflectively.

```java
Object proxy = Proxy.newProxyInstance(loader, interfaces, handler);
```

- `java.lang.reflect.Proxy`: every method and constructor
- `java.lang.reflect.InvocationHandler`: `invokeDefault`
- `java.lang.invoke.MethodHandleProxies`: every method and constructor

### `HANDLES`

A method or variable handle reaches a member as reflection does, resolving it by name through a lookup, and the factories and bootstraps behind lambdas, string concatenation, records and switches spin classes at run time. All of `java.lang.invoke` is reported, but for the exceptions it declares, together with the bootstraps of `java.lang.runtime`, the resolution of `java.lang.constant` descriptions, and the handles of `java.lang.foreign.Linker`.

```java
MethodHandles.Lookup lookup = MethodHandles.lookup();
VarHandle next = lookup.findVarHandle(Node.class, "next", Node.class);
```

- Every type of `java.lang.invoke` and the packages below it, but its exceptions: every method and constructor
- `java.lang.runtime.ObjectMethods`: every method and constructor
- `java.lang.runtime.SwitchBootstraps`: every method and constructor
- `java.lang.constant.ConstantDesc` and every subtype of it: `resolveConstantDesc`
- `java.lang.constant.DynamicCallSiteDesc`: `resolveCallSiteDesc`
- `java.lang.foreign.Linker` and every subtype of it: `downcallHandle`, `upcallStub`

### `SERVICE_LOADING`

A service loader finds implementation classes by name and instantiates them reflectively.

```java
ServiceLoader<Codec> codecs = ServiceLoader.load(Codec.class);
```

- `java.util.ServiceLoader`: every method and constructor
- `java.util.ServiceLoader.Provider` and every subtype of it: `get`
- `io.micronaut.core.io.service.SoftServiceLoader`: every method and constructor

### `CLASS_LOADING`

A class loaded by name is out of sight of a native image, and a class defined at run time is not in one at all.

```java
Class<?> plugin = Class.forName("com.example.Plugin");
```

- `java.lang.Class`: `forName`
- `java.lang.ClassLoader` and every subtype of it: `loadClass`, `findClass`, `findLoadedClass`, `findSystemClass`, `defineClass`, `resolveClass`
- `java.lang.ModuleLayer`: `defineModules`, `defineModulesWithOneLoader`, `defineModulesWithManyLoaders`
- `java.util.ResourceBundle`: `getBundle`
- `java.util.ResourceBundle.Control` and every subtype of it: `newBundle`
- `io.micronaut.core.reflect.ClassUtils`: `forName`, `isPresent`

### `FIELD_UPDATERS`

The atomic field updaters look their field up with `getDeclaredField`, filling the declared fields of the class. A `VarHandle`, their replacement, is reported as `HANDLES`.

```java
AtomicReferenceFieldUpdater<Node, Node> next = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "next");
```

- `java.util.concurrent.atomic.AtomicReferenceFieldUpdater`: `newUpdater`
- `java.util.concurrent.atomic.AtomicIntegerFieldUpdater`: `newUpdater`
- `java.util.concurrent.atomic.AtomicLongFieldUpdater`: `newUpdater`

### `ENUM_CONSTANTS`

`Class.getEnumConstants` invokes the `values()` of an enum reflectively and keeps the constants for the class, and `Enum.valueOf` builds and keeps a map of them by name. `EnumSet` and `EnumMap` take the constants of their type the same way, and the `valueOf(String)` the compiler writes for every enum calls `Enum.valueOf` - so `Colour.valueOf("RED")` is reported, and `Colour.values()` is not.

```java
Colour colour = Colour.valueOf("RED");
EnumSet<Colour> colours = EnumSet.noneOf(Colour.class);
```

- `java.lang.Class`: `getEnumConstants`
- `java.lang.Enum`: `valueOf`, `describeConstable`
- `java.lang.Enum.EnumDesc`: `of`
- `java.util.EnumSet`: `noneOf`, `allOf`, `of`, `range`, `copyOf`
- `java.util.EnumMap`: its constructors
- Every enum: the `valueOf(String)` the compiler writes for it

### `CLASS_NAMES`

The simple and canonical names of a class are not in its class file as such. The first call works them out and keeps them in the reflection data of the class. `Class.getName` reads what the class file holds and is not reported.

```java
String name = type.getSimpleName();
```

- `java.lang.Class`: `getSimpleName`, `getCanonicalName`

### `INTERFACES`

`Class.getInterfaces` keeps the interfaces it returns in the reflection data of the class. `Class.getSuperclass` does not, and is not reported.

```java
Class<?>[] interfaces = type.getInterfaces();
```

- `java.lang.Class`: `getInterfaces`
- `io.micronaut.core.reflect.ClassUtils`: `resolveHierarchy`

### `GENERIC_SIGNATURES`

A generic signature is parsed from the class file on first use and kept for the class or the member, and the types it produces load the classes they name - which is why every method of a generic type is reported too.

```java
Type superclass = type.getGenericSuperclass();
```

- `java.lang.Class`: `getGenericSuperclass`, `getGenericInterfaces`, `getTypeParameters`, `toGenericString`
- `java.lang.reflect.Executable` and every subtype of it: `getGenericParameterTypes`, `getGenericExceptionTypes`, `getGenericReturnType`, `getTypeParameters`, `toGenericString`
- `java.lang.reflect.Field`: `getGenericType`, `toGenericString`
- `java.lang.reflect.RecordComponent`: `getGenericType`, `getGenericSignature`
- `java.lang.reflect.Parameter`: `getParameterizedType`
- `java.lang.reflect.ParameterizedType` and every subtype of it: every method and constructor
- `java.lang.reflect.TypeVariable` and every subtype of it: every method and constructor
- `java.lang.reflect.WildcardType` and every subtype of it: every method and constructor
- `java.lang.reflect.GenericArrayType` and every subtype of it: every method and constructor
- `io.micronaut.core.reflect.GenericTypeUtils`: every method and constructor

### `ANNOTATIONS`

Annotations read from a class, a member, a parameter or a type use are parsed from the class file into proxy instances and kept for it. Micronaut's annotation metadata answers the same questions from what was compiled, and is not reported.

```java
Deprecated deprecated = type.getAnnotation(Deprecated.class);
```

- `java.lang.reflect.AnnotatedElement` and every subtype of it: `getAnnotation`, `getAnnotations`, `getDeclaredAnnotation`, `getDeclaredAnnotations`, `getAnnotationsByType`, `getDeclaredAnnotationsByType`, `isAnnotationPresent`
- `java.lang.Class`: `getAnnotatedSuperclass`, `getAnnotatedInterfaces`
- `java.lang.reflect.Executable` and every subtype of it: `getParameterAnnotations`, `getAnnotatedReturnType`, `getAnnotatedReceiverType`, `getAnnotatedParameterTypes`, `getAnnotatedExceptionTypes`
- `java.lang.reflect.Method`: `getDefaultValue`
- `java.lang.reflect.Field`: `getAnnotatedType`
- `java.lang.reflect.RecordComponent`: `getAnnotatedType`
- `java.lang.reflect.Parameter`: `getAnnotatedType`
- `java.lang.reflect.AnnotatedType` and every subtype of it: every method and constructor

### `CLASS_MEMBERS`

Looking a member up fills the reflection data of its class with every member of that kind, declared or public, each a `java.lang.reflect` object.

```java
Method[] methods = type.getDeclaredMethods();
```

- `java.lang.Class`: `getMethod`, `getMethods`, `getDeclaredMethod`, `getDeclaredMethods`, `getConstructor`, `getConstructors`, `getDeclaredConstructor`, `getDeclaredConstructors`, `getField`, `getFields`, `getDeclaredField`, `getDeclaredFields`, `getRecordComponents`, `getPermittedSubclasses`, `getNestMembers`, `getClasses`, `getDeclaredClasses`, `getEnclosingMethod`, `getEnclosingConstructor`
- `java.lang.reflect.RecordComponent`: `getAccessor`
- `java.lang.reflect.Executable` and every subtype of it: `getParameters`

### `REFLECTIVE_ACCESS`

Reaching a member once it is found - invoking a method, reading or writing the value of a field, creating an instance - makes and keeps an accessor for it, and `setAccessible` and `Module.addOpens` break encapsulation to allow it. Reading the name, type or modifiers of a member already at hand is not reported.

```java
Object value = field.get(target);
```

- `java.lang.reflect.AccessibleObject` and every subtype of it: `setAccessible`, `trySetAccessible`, `canAccess`, `isAccessible`
- `java.lang.reflect.Constructor`: `newInstance`
- `java.lang.reflect.Method`: `invoke`
- `java.lang.reflect.Field`: `get`, `getBoolean`, `getByte`, `getChar`, `getShort`, `getInt`, `getLong`, `getFloat`, `getDouble`, `set`, `setBoolean`, `setByte`, `setChar`, `setShort`, `setInt`, `setLong`, `setFloat`, `setDouble`
- `java.lang.reflect.Array`: every method and constructor
- `java.lang.Class`: `newInstance`
- `java.lang.Module`: `addOpens`
- `java.lang.ModuleLayer.Controller`: `addOpens`
- `io.micronaut.core.reflect.InstantiationUtils`: every method and constructor

### `CUSTOM`

The calls a build forbids besides the categories, with `NoReflection:ForbiddenCalls` or `forbid(...)` in the Gradle plugin. Typically a project's own helpers that reach for reflection.

```java
Object plugin = LegacyReflector.lookup("plugin"); // with forbid("com.example.LegacyReflector#*")
```
## How calls are matched

A call is matched by the method or the constructor the compiler resolved it to, not by how the source spells it. The same call is reported whether it is made directly, through a static import, as a method or constructor reference (`Class::getSimpleName`, `EnumMap::new`), as the constructor of an anonymous subclass or a `super(...)` call, through a subtype that declares or inherits the method (`method.getAnnotation(...)`, `loadClass(...)` in a `ClassLoader` subclass), and wherever it sits: a field initializer, an initializer, a lambda, or a nested, local or anonymous class.

A method of the same name on another type is not reported: `AnnotationMetadata.getAnnotation` reads what Micronaut compiled, `Colour.values()` returns the array the compiler wrote, and `Class.getName`, `Class.getSuperclass` and the name, type or modifiers of a member already looked up read the class file. The generic and annotated types a member hands out are the exception: their methods resolve what they describe, and are reported. A project's own method wrapping a reflective call is not reported where it is called - the call inside it is - unless the build forbids the wrapper with `NoReflection:ForbiddenCalls`.

## With ErrorProne's own checks

The plugin applies the same [`net.ltgt.errorprone`](https://github.com/tbroyer/gradle-errorprone-plugin) plugin a project may already use, so `NoReflection` runs in the same compilation as ErrorProne's built-in checks and any other plugin check, such as NullAway. The `noReflection` block only sets the severity and the `NoReflection:*` options of this check; everything the project configures in `options.errorprone` stays as it is.

```kotlin
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("net.ltgt.errorprone") version "5.1.1"
    id("io.micronaut.errorprone.no-reflection") version "1.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.14.1")
}

noReflection {
    allowIn("com.example.ReflectionAccess")
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "com.example")
    }
}
```

With the `pluginManagement` repositories shown above in the settings, a compilation then fails on both a reflective call outside `com.example.ReflectionAccess` and a null returned where NullAway forbids it. The ErrorProne the plugin adds and the one the project declares are resolved like any other dependency, to the higher of the two versions.

## Documentation

See the [Documentation](https://micronaut-projects.github.io/errorprone-no-reflection/latest/guide/) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/errorprone-no-reflection/snapshot/guide/) for the current development docs.

## Snapshots and Releases

Snapshots are automatically published to [Sonatype Snapshots](https://s01.oss.sonatype.org/content/repositories/snapshots/io/micronaut/) using [GitHub Actions](https://github.com/micronaut-projects/errorprone-no-reflection/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to Maven Central via [GitHub Actions](https://github.com/micronaut-projects/errorprone-no-reflection/actions).

Releases are completely automated. To perform a release use the following steps:

* [Publish the draft release](https://github.com/micronaut-projects/errorprone-no-reflection/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/errorprone-no-reflection/actions?query=workflow%3ARelease) to check it passed successfully.
* If everything went fine, [publish to Maven Central](https://github.com/micronaut-projects/errorprone-no-reflection/actions?query=workflow%3A"Maven+Central+Sync").
* Celebrate!
