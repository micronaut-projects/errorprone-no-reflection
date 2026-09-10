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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Calls named the way a build writes them: a type or a package, then {@code #} and the members.
 *
 * <ul>
 *     <li>{@code java.lang.Class#getSimpleName} - a method of the class, whatever its parameters</li>
 *     <li>{@code java.lang.Class#getSimpleName|getCanonicalName} - either of the two</li>
 *     <li>{@code java.util.EnumMap#<init>} - the constructors of the class</li>
 *     <li>{@code java.util.EnumSet#*}, or {@code java.util.EnumSet} alone - any method or constructor of it</li>
 *     <li>{@code io.micronaut.core.reflect.ReflectionUtils#*-getWrapperType|getPrimitiveType} - any but those</li>
 *     <li>{@code java.lang.reflect.AnnotatedElement+#getAnnotation} - declared by the type or by a subtype of it</li>
 *     <li>{@code java.lang.invoke.*#*} - of a type of the package, or of a package below it</li>
 * </ul>
 *
 * <p>A nested type is named either way, {@code java.util.ServiceLoader.Provider} or
 * {@code java.util.ServiceLoader$Provider}. A pattern holds names only; the checks match it against the method the
 * compiler resolved a call, a method reference or a constructor to.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
public final class CallPattern {

    /** A qualified name, whose segments are identifiers, a nested class possibly joined with {@code $}. */
    static final Pattern QUALIFIED_NAME = Pattern.compile(
        "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

    private static final Pattern MEMBER = Pattern.compile("<init>|\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

    /** How the type of a pattern is matched against the class declaring a member. */
    public enum OwnerMatch {
        /** The class itself. */
        EXACT,
        /** The class or any subtype of it. */
        DESCENDANTS,
        /** Any class of the package, or of a package below it. */
        PACKAGE
    }

    private final String text;
    private final String owner;
    private final OwnerMatch ownerMatch;
    private final Set<String> members;
    private final Set<String> excluded;

    private CallPattern(String text, String owner, OwnerMatch ownerMatch, Set<String> members, Set<String> excluded) {
        this.text = text;
        this.owner = owner;
        this.ownerMatch = ownerMatch;
        this.members = members;
        this.excluded = excluded;
    }

    /**
     * @param text The pattern
     * @return The pattern parsed
     * @throws IllegalArgumentException When it is not a pattern, saying why and what one looks like
     */
    public static CallPattern parse(String text) {
        String pattern = text.strip();
        int hash = pattern.indexOf('#');
        String type = hash < 0 ? pattern : pattern.substring(0, hash);
        String member = hash < 0 ? "*" : pattern.substring(hash + 1);
        OwnerMatch match = OwnerMatch.EXACT;
        if (type.endsWith(".*")) {
            match = OwnerMatch.PACKAGE;
            type = type.substring(0, type.length() - 2);
        } else if (type.endsWith("+")) {
            match = OwnerMatch.DESCENDANTS;
            type = type.substring(0, type.length() - 1);
        }
        if (!QUALIFIED_NAME.matcher(type).matches()) {
            throw invalid(text, "'" + type + "' is not a qualified type or package name");
        }
        Set<String> members = Set.of();
        Set<String> excluded = Set.of();
        if (member.startsWith("*")) {
            String rest = member.substring(1);
            if (!rest.isEmpty()) {
                if (rest.charAt(0) != '-') {
                    throw invalid(text, "'*' is followed by '" + rest + "' rather than by '-' and the members it leaves out");
                }
                excluded = names(text, rest.substring(1));
            }
        } else {
            members = names(text, member);
        }
        return new CallPattern(pattern, type, match, members, excluded);
    }

    private static Set<String> names(String text, String names) {
        Set<String> result = new LinkedHashSet<>();
        for (String name : names.split("\\|", -1)) {
            if (!MEMBER.matcher(name).matches()) {
                throw invalid(text, "'" + name + "' is not the name of a method, nor <init>");
            }
            result.add(name);
        }
        return Set.copyOf(result);
    }

    private static IllegalArgumentException invalid(String text, String why) {
        return new IllegalArgumentException("Invalid call pattern '" + text + "': " + why + ". A pattern is a type, a type "
            + "followed by + to take in its subtypes, or a package followed by .*; then # and the name of a method, names "
            + "separated by |, <init> for the constructors, or * for any member.");
    }

    /**
     * @return The type or package named
     */
    public String owner() {
        return owner;
    }

    /**
     * @return How the type is matched
     */
    public OwnerMatch ownerMatch() {
        return ownerMatch;
    }

    /**
     * @param name The simple name of a method, or {@code <init>}
     * @return Whether the pattern names it
     */
    public boolean matchesMember(String name) {
        return (members.isEmpty() || members.contains(name)) && !excluded.contains(name);
    }

    /**
     * @param qualifiedName The qualified name of a class
     * @param flatName      Its flat name, with a nested class joined by {@code $}
     * @return Whether the class is the type the pattern names
     */
    public boolean namesType(String qualifiedName, String flatName) {
        return owner.equals(qualifiedName) || owner.equals(flatName);
    }

    /**
     * @param packageName The name of a package
     * @return Whether it is the package the pattern names or a package below it
     */
    public boolean coversPackage(String packageName) {
        return packageName.equals(owner) || packageName.startsWith(owner + ".");
    }

    @Override
    public String toString() {
        return text;
    }
}
