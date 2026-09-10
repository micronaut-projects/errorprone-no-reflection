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
package io.micronaut.errorprone.noreflection.gradle;

import io.micronaut.errorprone.noreflection.AllowedScope;
import io.micronaut.errorprone.noreflection.CallPattern;
import io.micronaut.errorprone.noreflection.ReflectionCategory;
import net.ltgt.gradle.errorprone.CheckSeverity;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * How a project forbids reflection.
 *
 * <pre>{@code
 * noReflection {
 *     // every category is reported by default, and a single call may be suppressed on the variable holding its result
 *
 *     allow 'ENUM_CONSTANTS'                          // a category, throughout the project
 *     allowCalls 'java.lang.Class#getSimpleName'      // a method, throughout the project
 *     allowIn 'com.example.ReflectionAccess'          // any reflection, in a class
 *     allowIn 'com.example.handles.*', 'HANDLES'      // a category, in a package and the packages below it
 *     forbid 'com.example.LegacyReflector#*'          // the project's own reflective helpers, reported as CUSTOM
 *
 *     suppressible = false                            // no suppression in the source is honoured
 *     forbidAll()                                     // nothing allowed but in allowIn, and nothing suppressible
 *     severity 'WARN'                                 // reported without failing the compilation
 *     errorProneVersion = '2.50.0'                    // the ErrorProne the plugin adds
 *     checkedSourceSets = ['main', 'integration']     // the source sets checked, main by default
 *     checkTests = true                               // every source set checked
 * }
 * }</pre>
 *
 * <p>The categories are the names of {@link ReflectionCategory}, calls are written as {@link CallPattern}s and classes
 * and packages as {@link AllowedScope}s.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
public abstract class NoReflectionExtension {

    /**
     * @return The categories allowed throughout the project, none by default
     */
    public abstract SetProperty<ReflectionCategory> getAllowed();

    /**
     * @return The calls allowed throughout the project, as {@link CallPattern}s, none by default
     */
    public abstract SetProperty<String> getAllowedCalls();

    /**
     * @return The classes and packages reflection is allowed in, by the class or the package followed by {@code .*},
     * with the categories allowed there, all of them when the set is empty; none by default
     */
    public abstract MapProperty<String, Set<ReflectionCategory>> getAllowedIn();

    /**
     * @return The calls reported besides the categories, as {@link CallPattern}s, none by default
     */
    public abstract SetProperty<String> getForbiddenCalls();

    /**
     * @return Whether a {@code @SuppressWarnings("NoReflection")} in the source is honoured, {@code true} by default
     */
    public abstract Property<Boolean> getSuppressible();

    /**
     * @return Whether all reflection is forbidden but in the classes and packages of {@link #getAllowedIn()}, with no
     * suppression honoured, {@code false} by default
     */
    public abstract Property<Boolean> getForbidAll();

    /**
     * @return The severity reflection is reported with, {@code ERROR} by default
     */
    public abstract Property<CheckSeverity> getSeverity();

    /**
     * @return The names of the source sets whose compilation is checked, {@code main} by default
     */
    public abstract SetProperty<String> getCheckedSourceSets();

    /**
     * @return The version of ErrorProne added to the project, the one the plugin was built with by default; a version the
     * project declares itself is resolved against it as any other dependency is
     */
    public abstract Property<String> getErrorProneVersion();

    /**
     * @return Whether every source set is checked, the tests included, {@code false} by default: a test that proves
     * something was reached without reflection has to reach it somehow
     */
    public abstract Property<Boolean> getCheckTests();

    /**
     * Allows the given categories throughout the project.
     *
     * @param categories The categories
     */
    public void allow(ReflectionCategory... categories) {
        getAllowed().addAll(categories);
    }

    /**
     * Allows the named categories throughout the project.
     *
     * @param categories The names of the categories, as {@link ReflectionCategory} spells them
     */
    public void allow(String... categories) {
        for (String category : categories) {
            getAllowed().add(category(category));
        }
    }

    /**
     * Allows the given calls throughout the project.
     *
     * @param patterns The calls, as {@link CallPattern}s
     */
    public void allowCalls(String... patterns) {
        for (String pattern : patterns) {
            getAllowedCalls().add(pattern(pattern).toString());
        }
    }

    /**
     * Allows reflection in a class, or in a package and the packages below it. Naming the same scope again adds to the
     * categories allowed there, and a scope allowed every category keeps them all.
     *
     * @param scope      The class, or the package followed by {@code .*}
     * @param categories The categories allowed there, all of them when none is given
     */
    public void allowIn(String scope, String... categories) {
        AllowedScope parsed;
        try {
            parsed = AllowedScope.parse(scope);
        } catch (IllegalArgumentException e) {
            throw new InvalidUserDataException("noReflection: " + e.getMessage(), e);
        }
        Set<ReflectionCategory> requested = EnumSet.noneOf(ReflectionCategory.class);
        requested.addAll(parsed.categories());
        for (String category : categories) {
            requested.add(category(category));
        }
        Set<ReflectionCategory> current = getAllowedIn().getting(parsed.scope()).getOrNull();
        Set<ReflectionCategory> merged;
        if (current == null) {
            merged = requested;
        } else if (current.isEmpty() || requested.isEmpty()) {
            merged = Set.of();
        } else {
            merged = EnumSet.copyOf(current);
            merged.addAll(requested);
        }
        getAllowedIn().put(parsed.scope(), Set.copyOf(merged));
    }

    /**
     * Reports the given calls, as the category {@link ReflectionCategory#CUSTOM}, besides the categories.
     *
     * @param patterns The calls, as {@link CallPattern}s
     */
    public void forbid(String... patterns) {
        for (String pattern : patterns) {
            getForbiddenCalls().add(pattern(pattern).toString());
        }
    }

    /**
     * Forbids all reflection but in the classes and packages of {@link #allowIn(String, String...)}, with no
     * suppression honoured.
     */
    public void forbidAll() {
        getForbidAll().set(true);
    }

    /**
     * Sets the severity reflection is reported with.
     *
     * @param severity {@code ERROR} or {@code WARN}
     */
    public void severity(String severity) {
        String normalized = severity.strip().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ERROR") && !normalized.equals("WARN")) {
            throw new InvalidUserDataException("noReflection: the severity is ERROR or WARN, not '" + severity + "'.");
        }
        getSeverity().set(CheckSeverity.valueOf(normalized));
    }

    private static ReflectionCategory category(String name) {
        try {
            return ReflectionCategory.named(name);
        } catch (IllegalArgumentException e) {
            throw new InvalidUserDataException("noReflection: " + e.getMessage(), e);
        }
    }

    private static CallPattern pattern(String pattern) {
        try {
            return CallPattern.parse(pattern);
        } catch (IllegalArgumentException e) {
            throw new InvalidUserDataException("noReflection: " + e.getMessage(), e);
        }
    }
}
