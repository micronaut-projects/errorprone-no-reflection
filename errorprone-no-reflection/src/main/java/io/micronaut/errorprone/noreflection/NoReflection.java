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

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import javax.inject.Inject;
import java.util.Set;

/**
 * Reports reflection: a call, a method named rather than called, or a constructor that reaches for it, unless the build
 * allows it.
 *
 * <p>A method reference is matched as well as a call, and a constructor as well as a method: {@code Class::getDeclaredMethods}
 * handed to a map is the lookup deferred, and {@code new EnumMap<>(type)} fills the enum constants of the type as surely
 * as {@code EnumSet.noneOf} does. What the build allows and forbids comes from the flags declared on
 * {@link ReflectionCategory}.</p>
 *
 * <p>A single call can still be allowed where nothing else will do, with a suppression on the local variable that holds
 * what the platform returned rather than on the method, so that the rest of the method stays checked:</p>
 *
 * <pre>{@code
 * public Method getMethod() {
 *     // getMethod returns a java.lang.reflect.Method, which only the platform can produce
 *     @SuppressWarnings("NoReflection")
 *     Method target = context.getExecutableMethod().getTargetMethod();
 *     return target;
 * }
 * }</pre>
 *
 * <p>The check decides suppression itself rather than leaving it to ErrorProne, so that a build can stop honouring it with
 * {@link ReflectionCategory#SUPPRESSIBLE_FLAG} for this check alone - ErrorProne's option to ignore suppression
 * annotations would do it for every check at once.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
@AutoService(BugChecker.class)
@BugPattern(
    name = "NoReflection",
    summary = "Reflection is not allowed here",
    explanation = """
        The project forbids reflection. Reaching for it fills the caches the virtual machine keeps for a class - its \
        reflection data, enum constants, generic signature and annotations, the accessors of its members - or loads, \
        defines or proxies classes, and needs reachability metadata in a native image.

        Where a kind of reflection is acceptable throughout the project, allow its category, the one named at the end \
        of the message: -XepOpt:NoReflection:Allowed=CATEGORY, or noReflection { allow("CATEGORY") } with the Gradle \
        plugin. Where it is acceptable in one class or package, NoReflection:AllowedIn=com.example.ReflectionAccess, \
        and where one method is, NoReflection:AllowedCalls=java.lang.Class#getSimpleName. Unless the build sets \
        NoReflection:Suppressible=false, one call with no alternative can be suppressed with \
        @SuppressWarnings("NoReflection") on the local variable holding the result; say above it why the platform had \
        to be asked. On the method instead, it would also hide every other call the method makes.""",
    severity = BugPattern.SeverityLevel.ERROR,
    // the check decides suppression itself, so that a build can stop honouring it for this check alone
    suppressionAnnotations = {})
public final class NoReflection extends BugChecker
    implements BugChecker.MethodInvocationTreeMatcher, BugChecker.MemberReferenceTreeMatcher, BugChecker.NewClassTreeMatcher {

    /** The flag naming the categories allowed throughout the project, separated by commas. */
    public static final String ALLOWED_FLAG = ReflectionCategory.ALLOWED_FLAG;

    private static final long serialVersionUID = 1L;

    /** The values of {@code @SuppressWarnings} that suppress the check, as ErrorProne would honour them. */
    private static final Set<String> SUPPRESSIONS = Set.of("NoReflection", "all");

    private final transient ReflectionPolicy policy;

    private final boolean suppressible;

    /**
     * Forbids every category everywhere, and honours a suppression.
     */
    public NoReflection() {
        this(ErrorProneFlags.empty());
    }

    /**
     * @param flags The flags naming what the build allows and forbids, and whether a suppression is honoured
     */
    @Inject
    public NoReflection(ErrorProneFlags flags) {
        policy = ReflectionPolicy.of(flags);
        suppressible = flags.getBoolean(ReflectionCategory.SUPPRESSIBLE_FLAG).orElse(true);
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        return describe(tree, ASTHelpers.getSymbol(tree), state);
    }

    @Override
    public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
        return describe(tree, ASTHelpers.getSymbol(tree), state);
    }

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
        return describe(tree, ASTHelpers.getSymbol(tree), state);
    }

    private Description describe(Tree tree, Symbol.MethodSymbol method, VisitorState state) {
        if (method == null) {
            return Description.NO_MATCH;
        }
        ReflectionCategory category = policy.reported(method, state);
        if (category == null || (suppressible && suppressed(state))) {
            return Description.NO_MATCH;
        }
        return buildDescription(tree).setMessage(message() + " [" + category + "]").build();
    }

    // a suppression on any declaration around the call: the variable holding its result, the method, a class
    private static boolean suppressed(VisitorState state) {
        for (Tree enclosing : state.getPath()) {
            Symbol symbol = switch (enclosing) {
                case VariableTree variable -> ASTHelpers.getSymbol(variable);
                case MethodTree method -> ASTHelpers.getSymbol(method);
                case ClassTree type -> ASTHelpers.getSymbol(type);
                default -> null;
            };
            SuppressWarnings suppression = symbol == null ? null : symbol.getAnnotation(SuppressWarnings.class);
            if (suppression != null) {
                for (String value : suppression.value()) {
                    if (SUPPRESSIONS.contains(value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
