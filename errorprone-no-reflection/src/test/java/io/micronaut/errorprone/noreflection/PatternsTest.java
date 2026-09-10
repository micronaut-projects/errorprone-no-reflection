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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a build writes calls and scopes, and what it is told when it writes them wrong.
 */
class PatternsTest {

    @Test
    void parsesEveryFormOfACallPattern() {
        CallPattern exact = CallPattern.parse(" java.lang.Class#getSimpleName|getCanonicalName ");
        assertEquals("java.lang.Class", exact.owner());
        assertEquals(CallPattern.OwnerMatch.EXACT, exact.ownerMatch());
        assertTrue(exact.matchesMember("getCanonicalName"));
        assertFalse(exact.matchesMember("getName"));

        CallPattern subtypes = CallPattern.parse("java.lang.reflect.AnnotatedElement+#*-getAnnotation");
        assertEquals(CallPattern.OwnerMatch.DESCENDANTS, subtypes.ownerMatch());
        assertTrue(subtypes.matchesMember("getAnnotations"));
        assertFalse(subtypes.matchesMember("getAnnotation"));

        CallPattern inPackage = CallPattern.parse("java.lang.invoke.*");
        assertEquals(CallPattern.OwnerMatch.PACKAGE, inPackage.ownerMatch());
        assertTrue(inPackage.matchesMember("anything"));
        assertTrue(inPackage.coversPackage("java.lang.invoke"));
        assertTrue(inPackage.coversPackage("java.lang.invoke.below"));
        assertFalse(inPackage.coversPackage("java.lang.invokes"));

        assertTrue(CallPattern.parse("java.util.EnumMap#<init>").matchesMember("<init>"));
        CallPattern nested = CallPattern.parse("java.util.ServiceLoader$Provider#get");
        assertTrue(nested.namesType("java.util.ServiceLoader.Provider", "java.util.ServiceLoader$Provider"));
    }

    @Test
    void refusesAMalformedCallPatternSayingWhy() {
        assertEquals("Invalid call pattern 'java.lang.Class#get Name': 'get Name' is not the name of a method, nor <init>. "
                + "A pattern is a type, a type followed by + to take in its subtypes, or a package followed by .*; then # "
                + "and the name of a method, names separated by |, <init> for the constructors, or * for any member.",
            assertThrows(IllegalArgumentException.class, () -> CallPattern.parse("java.lang.Class#get Name")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> CallPattern.parse("#getName"));
        assertThrows(IllegalArgumentException.class, () -> CallPattern.parse("java..Class#getName"));
        assertThrows(IllegalArgumentException.class, () -> CallPattern.parse("java.lang.Class#*getName"));
        assertThrows(IllegalArgumentException.class, () -> CallPattern.parse("java.lang.Class#getName|"));
    }

    @Test
    void parsesAScopeWithOrWithoutCategories() {
        AllowedScope handles = AllowedScope.parse("com.example.Handles:HANDLES+proxy");
        assertTrue(handles.allows(ReflectionCategory.HANDLES));
        assertTrue(handles.allows(ReflectionCategory.PROXY));
        assertFalse(handles.allows(ReflectionCategory.CLASS_MEMBERS));
        assertTrue(handles.coversClass("com.example.Handles", "com.example.Handles"));
        assertFalse(handles.coversPackage("com.example"));
        assertEquals("com.example.Handles:HANDLES+PROXY", handles.toFlag());

        AllowedScope internal = AllowedScope.parse("com.example.internal.*");
        assertTrue(internal.allows(ReflectionCategory.CLASS_MEMBERS));
        assertTrue(internal.coversPackage("com.example.internal.deep"));
        assertFalse(internal.coversPackage("com.example.internals"));
        assertFalse(internal.coversClass("com.example.internal", "com.example.internal"));
        assertEquals("com.example.internal.*", internal.toFlag());
    }

    @Test
    void refusesAnUnknownCategoryNamingTheOnesThereAre() {
        String message = assertThrows(IllegalArgumentException.class, () -> AllowedScope.parse("com.example.Handles:HANDEL"))
            .getMessage();
        assertTrue(message.startsWith("Unknown reflection category 'HANDEL'. The categories are [ANNOTATION_SYNTHESIS, "), message);
        assertTrue(message.contains("HANDLES"), message);
        assertThrows(IllegalArgumentException.class, () -> AllowedScope.parse("com example"));
    }
}
