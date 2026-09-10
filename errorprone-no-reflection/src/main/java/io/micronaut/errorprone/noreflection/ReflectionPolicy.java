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

import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * What a build allows and forbids, read from the flags of the checks.
 *
 * <p>A call is reported when it belongs to a category, or to a call the build forbids, unless its category is allowed
 * throughout the project, the call itself is, or it is made in a class or a package where its category is.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
final class ReflectionPolicy {

    private final Set<ReflectionCategory> allowed;
    private final List<CallPattern> allowedCalls;
    private final List<AllowedScope> allowedIn;
    private final List<CallPattern> forbiddenCalls;

    private ReflectionPolicy(Set<ReflectionCategory> allowed, List<CallPattern> allowedCalls, List<AllowedScope> allowedIn,
                             List<CallPattern> forbiddenCalls) {
        this.allowed = allowed;
        this.allowedCalls = allowedCalls;
        this.allowedIn = allowedIn;
        this.forbiddenCalls = forbiddenCalls;
    }

    /**
     * @param flags The flags of the check
     * @return What they allow and forbid
     */
    static ReflectionPolicy of(ErrorProneFlags flags) {
        Set<ReflectionCategory> allowed = EnumSet.noneOf(ReflectionCategory.class);
        flags.getListOrEmpty(ReflectionCategory.ALLOWED_FLAG).forEach(name -> allowed.add(ReflectionCategory.named(name)));
        return new ReflectionPolicy(Collections.unmodifiableSet(allowed),
            flags.getListOrEmpty(ReflectionCategory.ALLOWED_CALLS_FLAG).stream().map(CallPattern::parse).toList(),
            flags.getListOrEmpty(ReflectionCategory.ALLOWED_IN_FLAG).stream().map(AllowedScope::parse).toList(),
            flags.getListOrEmpty(ReflectionCategory.FORBIDDEN_CALLS_FLAG).stream().map(CallPattern::parse).toList());
    }

    /**
     * @param method The method, or constructor, a call or a reference resolved to
     * @param state  The state, whose path leads to the call
     * @return The category to report the call under, or {@code null} when it is not reported
     */
    ReflectionCategory reported(Symbol.MethodSymbol method, VisitorState state) {
        ReflectionCategory category = ReflectionMatchers.categoryOf(method, state);
        if (category == null && anyMatches(forbiddenCalls, method, state)) {
            category = ReflectionCategory.CUSTOM;
        }
        if (category == null || allowed.contains(category) || anyMatches(allowedCalls, method, state)
            || inAllowedScope(category, state)) {
            return null;
        }
        return category;
    }

    private static boolean anyMatches(List<CallPattern> patterns, Symbol.MethodSymbol method, VisitorState state) {
        for (CallPattern pattern : patterns) {
            if (ReflectionMatchers.matches(pattern, method, state)) {
                return true;
            }
        }
        return false;
    }

    private boolean inAllowedScope(ReflectionCategory category, VisitorState state) {
        if (allowedIn.isEmpty()) {
            return false;
        }
        String packageName = null;
        for (Tree enclosing : state.getPath()) {
            if (enclosing instanceof ClassTree classTree) {
                Symbol.ClassSymbol type = ASTHelpers.getSymbol(classTree);
                if (type == null) {
                    continue;
                }
                String qualifiedName = type.getQualifiedName().toString();
                String flatName = type.flatName().toString();
                packageName = type.packge().getQualifiedName().toString();
                for (AllowedScope scope : allowedIn) {
                    if (scope.allows(category) && scope.coversClass(qualifiedName, flatName)) {
                        return true;
                    }
                }
            }
        }
        if (packageName != null) {
            for (AllowedScope scope : allowedIn) {
                if (scope.allows(category) && scope.coversPackage(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
