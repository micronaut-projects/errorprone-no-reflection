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
import io.micronaut.errorprone.noreflection.ReflectionCategory;
import net.ltgt.gradle.errorprone.CheckSeverity;
import net.ltgt.gradle.errorprone.ErrorProneOptions;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reports reflection in a project, except where the project allows it.
 *
 * <p>Applies ErrorProne, adds the {@code NoReflection} check of {@code micronaut-errorprone-no-reflection} to it, and hands the
 * check what the project allows and forbids as the flags declared on {@link ReflectionCategory}. See
 * {@link NoReflectionExtension} for the configuration.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.0
 */
public class NoReflectionPlugin implements Plugin<Project> {

    /** The artifact of the check, in the group and of the version of the plugin. */
    public static final String CHECKS_ARTIFACT = "micronaut-errorprone-no-reflection";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("net.ltgt.errorprone");
        Properties versions = versions();

        NoReflectionExtension extension = project.getExtensions().create("noReflection", NoReflectionExtension.class);
        extension.getAllowed().convention(Set.of());
        extension.getAllowedCalls().convention(Set.of());
        extension.getAllowedIn().convention(Map.of());
        extension.getForbiddenCalls().convention(Set.of());
        extension.getSuppressible().convention(true);
        extension.getForbidAll().convention(false);
        extension.getSeverity().convention(CheckSeverity.ERROR);
        extension.getCheckedSourceSets().convention(Set.of(SourceSet.MAIN_SOURCE_SET_NAME));
        extension.getCheckedTasks().convention(Set.of());
        extension.getCheckTests().convention(false);
        extension.getErrorProneVersion().convention(versions.getProperty("errorProne"));

        project.getDependencies().add("errorprone",
            versions.getProperty("group") + ":" + CHECKS_ARTIFACT + ":" + versions.getProperty("checks"));
        project.getDependencies().addProvider("errorprone",
            extension.getErrorProneVersion().map(version -> "com.google.errorprone:error_prone_core:" + version));

        project.getTasks().withType(JavaCompile.class).configureEach(task -> configure(project, task, extension));
    }

    private static void configure(Project project, JavaCompile task, NoReflectionExtension extension) {
        ErrorProneOptions errorProne = ((ExtensionAware) task.getOptions()).getExtensions().getByType(ErrorProneOptions.class);
        String taskName = task.getName();
        Provider<Boolean> checked = project.provider(() ->
            checked(project.getExtensions().findByType(SourceSetContainer.class), taskName, extension));
        Provider<CheckSeverity> severity = extension.getSeverity().map(NoReflectionPlugin::validSeverity);
        errorProne.getChecks().put("NoReflection", checked.zip(severity, (on, chosen) -> on ? chosen : CheckSeverity.OFF));
        errorProne.getCheckOptions().putAll(project.provider(() -> options(extension)));
    }

    private static boolean checked(SourceSetContainer sourceSets, String taskName, NoReflectionExtension extension) {
        if (extension.getCheckTests().get()) {
            return true;
        }
        if (sourceSets != null) {
            for (SourceSet sourceSet : sourceSets) {
                if (sourceSet.getCompileJavaTaskName().equals(taskName)) {
                    return extension.getCheckedSourceSets().get().contains(sourceSet.getName());
                }
            }
        }
        // a compilation that belongs to no source set is checked when the build names it
        return extension.getCheckedTasks().get().contains(taskName);
    }

    // the property takes any severity ErrorProne knows, of which OFF and DEFAULT would quietly turn the check off
    private static CheckSeverity validSeverity(CheckSeverity severity) {
        if (severity != CheckSeverity.ERROR && severity != CheckSeverity.WARN) {
            throw new GradleException("noReflection: the severity is ERROR or WARN, not " + severity + ".");
        }
        return severity;
    }

    private static Map<String, String> options(NoReflectionExtension extension) {
        Set<ReflectionCategory> allowed = extension.getAllowed().get();
        Set<String> allowedCalls = extension.getAllowedCalls().get();
        Map<String, Set<ReflectionCategory>> allowedIn = extension.getAllowedIn().get();
        Set<String> forbiddenCalls = extension.getForbiddenCalls().get();
        boolean forbidAll = extension.getForbidAll().get();
        if (forbidAll && (!allowed.isEmpty() || !allowedCalls.isEmpty())) {
            List<String> throughout = new ArrayList<>();
            allowed.stream().map(Enum::name).sorted().forEach(throughout::add);
            allowedCalls.stream().sorted().forEach(throughout::add);
            throw new GradleException("noReflection: forbidAll() allows no reflection outside the classes and packages "
                + "named with allowIn(...), but " + throughout + " were allowed throughout the project. Keep one or the other.");
        }
        Map<String, String> options = new LinkedHashMap<>();
        put(options, ReflectionCategory.ALLOWED_FLAG, allowed.stream().map(Enum::name));
        put(options, ReflectionCategory.ALLOWED_CALLS_FLAG, allowedCalls.stream());
        put(options, ReflectionCategory.ALLOWED_IN_FLAG, allowedIn.entrySet().stream()
            .map(entry -> scope(entry.getKey(), entry.getValue())));
        put(options, ReflectionCategory.FORBIDDEN_CALLS_FLAG, forbiddenCalls.stream());
        if (forbidAll || !extension.getSuppressible().get()) {
            options.put(ReflectionCategory.SUPPRESSIBLE_FLAG, "false");
        }
        return options;
    }

    private static String scope(String scope, Set<ReflectionCategory> categories) {
        String categoryNames = categories.stream().map(Enum::name).sorted().collect(Collectors.joining("+"));
        return AllowedScope.parse(categoryNames.isEmpty() ? scope : scope + ":" + categoryNames).toFlag();
    }

    private static void put(Map<String, String> options, String flag, Stream<String> values) {
        String joined = values.sorted().collect(Collectors.joining(","));
        if (!joined.isEmpty()) {
            options.put(flag, joined);
        }
    }

    private static Properties versions() {
        try (InputStream in = NoReflectionPlugin.class.getResourceAsStream("versions.properties")) {
            if (in == null) {
                throw new IllegalStateException("The errorprone-no-reflection plugin does not carry the versions it adds");
            }
            Properties versions = new Properties();
            versions.load(in);
            return versions;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
