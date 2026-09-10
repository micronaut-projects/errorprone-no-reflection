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

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class or a package in which reflection is allowed, as a build names it, with the only categories allowed there
 * when not all of them are.
 *
 * <ul>
 *     <li>{@code com.example.ReflectionAccess} - any reflection in the class, and in the classes and lambdas nested in it</li>
 *     <li>{@code com.example.Outer.Inner} or {@code com.example.Outer$Inner} - in a nested class</li>
 *     <li>{@code com.example.internal.*} - in any class of the package, or of a package below it</li>
 *     <li>{@code com.example.Handles:HANDLES+PROXY} - only those categories, in the class</li>
 * </ul>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
public final class AllowedScope {

    private final String name;
    private final boolean isPackage;
    private final Set<ReflectionCategory> categories;

    private AllowedScope(String name, boolean isPackage, Set<ReflectionCategory> categories) {
        this.name = name;
        this.isPackage = isPackage;
        this.categories = categories;
    }

    /**
     * @param text The scope
     * @return The scope parsed
     * @throws IllegalArgumentException When it is not a scope or names an unknown category, saying why
     */
    public static AllowedScope parse(String text) {
        String scope = text.strip();
        Set<ReflectionCategory> categories = EnumSet.noneOf(ReflectionCategory.class);
        int colon = scope.indexOf(':');
        if (colon >= 0) {
            for (String category : scope.substring(colon + 1).split("\\+", -1)) {
                categories.add(ReflectionCategory.named(category));
            }
            scope = scope.substring(0, colon);
        }
        boolean isPackage = scope.endsWith(".*");
        String name = isPackage ? scope.substring(0, scope.length() - 2) : scope;
        if (!CallPattern.QUALIFIED_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid scope '" + text + "': '" + name + "' is not a qualified class or "
                + "package name. A scope is a class, or a package followed by .*, then optionally : and the categories "
                + "allowed there, separated by +.");
        }
        return new AllowedScope(name, isPackage, Set.copyOf(categories));
    }

    /**
     * @return The class, or the package followed by {@code .*}, without the categories
     */
    public String scope() {
        return isPackage ? name + ".*" : name;
    }

    /**
     * @return The categories allowed, all of them when empty
     */
    public Set<ReflectionCategory> categories() {
        return categories;
    }

    /**
     * @param category A category
     * @return Whether it is allowed in the scope
     */
    public boolean allows(ReflectionCategory category) {
        return categories.isEmpty() || categories.contains(category);
    }

    /**
     * @param qualifiedName The qualified name of a class
     * @param flatName      Its flat name, with a nested class joined by {@code $}
     * @return Whether the scope is that class
     */
    public boolean coversClass(String qualifiedName, String flatName) {
        return !isPackage && (name.equals(qualifiedName) || name.equals(flatName));
    }

    /**
     * @param packageName The name of a package
     * @return Whether the scope is that package or a package above it
     */
    public boolean coversPackage(String packageName) {
        return isPackage && (packageName.equals(name) || packageName.startsWith(name + "."));
    }

    /**
     * @return The scope as the flag of the checks takes it
     */
    public String toFlag() {
        return scope() + (categories.isEmpty() ? ""
            : ":" + categories.stream().map(Enum::name).sorted().collect(Collectors.joining("+")));
    }

    @Override
    public String toString() {
        return toFlag();
    }
}
