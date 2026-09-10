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
package io.micronaut.errorprone.noreflection.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path
import java.util.regex.Pattern

class NoReflectionPluginFunctionalTest extends Specification {

    // the check this build has just built, which a fixture uses in place of the version the plugin adds
    private static final String CHECKS = System.getProperty('noreflection.checks').replace('\\', '/')
    private static final String VERSION = System.getProperty('noreflection.version')
    private static final String ERROR_PRONE_VERSION = System.getProperty('noreflection.errorProneVersion')

    @TempDir
    Path projectDir

    BuildResult result

    void "adds the check of its own version and ErrorProne to the project"() {
        given:
        file('settings.gradle').text = "rootProject.name = 'demo'"
        file('build.gradle').text = verifyDependencies('', "io.micronaut.errorprone:micronaut-errorprone-no-reflection:${VERSION}", "com.google.errorprone:error_prone_core:${ERROR_PRONE_VERSION}")

        when:
        run 'verifyDependencies'

        then:
        noExceptionThrown()
    }

    void "the version of ErrorProne added can be chosen"() {
        given:
        file('settings.gradle').text = "rootProject.name = 'demo'"
        file('build.gradle').text = verifyDependencies("errorProneVersion = '2.48.0'", 'com.google.errorprone:error_prone_core:2.48.0')

        when:
        run 'verifyDependencies'

        then:
        noExceptionThrown()
    }

    void "reports reflection, naming the category of the call"() {
        given:
        groovyProject()
        mainSubject '''
            Object members(Class<?> type) {
                return type.getDeclaredMethods();
            }
        '''

        when:
        fails 'compileJava'

        then:
        reported '[CLASS_MEMBERS]'
        reportsAt SUBJECT, 'getDeclaredMethods'
    }

    void "reports a call that fills a cache of the virtual machine, and all of java.lang.invoke"() {
        given:
        groovyProject()
        mainSubject '''
            String name(Class<?> type) {
                return type.getSimpleName();
            }

            Object handles() {
                return java.lang.invoke.MethodType.methodType(void.class);
            }
        '''

        when:
        fails 'compileJava'

        then:
        reported '[CLASS_NAMES]'
        reported '[HANDLES]'
    }

    void "a suppression on the variable holding the result allows that call"() {
        given:
        groovyProject()
        mainSubject '''
            Object members(Class<?> type) {
                @SuppressWarnings("NoReflection")
                Object members = type.getDeclaredMethods();
                return members;
            }
        '''

        when:
        run 'compileJava'

        then:
        noExceptionThrown()
    }

    void "an allowed category is not reported, and every other one still is"() {
        given:
        groovyProject "allow 'CLASS_MEMBERS'"
        mainSubject '''
            Object members(Class<?> type) throws Exception {
                Object methods = type.getDeclaredMethods();
                return methods == null ? null : Class.forName("demo.Subject");
            }
        '''

        when:
        fails 'compileJava'

        then:
        reported '[CLASS_LOADING]'
        !reportsAt(SUBJECT, 'getDeclaredMethods')
    }

    void "an allowed call is not reported, and a call the project forbids is"() {
        given:
        groovyProject """
            allowCalls 'java.lang.Class#getSimpleName'
            forbid 'demo.Legacy#lookup'
        """
        javaFile 'src/main/java/demo/Legacy.java', '''
            package demo;

            final class Legacy {
                static Object lookup(String name) {
                    return name;
                }
            }
        '''
        mainSubject '''
            Object names(Class<?> type) {
                String simple = type.getSimpleName();
                Object legacy = Legacy.lookup(simple);
                return legacy == null ? type.getCanonicalName() : legacy;
            }
        '''

        when:
        fails 'compileJava'

        then:
        reported '[CUSTOM]'
        reportsAt SUBJECT, 'Legacy.lookup'
        reportsAt SUBJECT, 'getCanonicalName'
        !reportsAt(SUBJECT, 'getSimpleName')
    }

    void "reflection allowed in one class is reported everywhere else"() {
        given:
        groovyProject "allowIn 'demo.ReflectionAccess'"
        javaFile REFLECTION_ACCESS, '''
            package demo;

            final class ReflectionAccess {
                static Object methods(Class<?> type) {
                    return type.getDeclaredMethods();
                }
            }
        '''
        mainSubject '''
            Object fields(Class<?> type) {
                return type.getDeclaredFields();
            }
        '''

        when:
        fails 'compileJava'

        then:
        reportsAt SUBJECT, 'getDeclaredFields'
        !reportsAt(REFLECTION_ACCESS, 'getDeclaredMethods')
    }

    void "a category can be allowed in a package only"() {
        given:
        groovyProject "allowIn 'demo.handles.*', 'HANDLES'"
        javaFile HANDLES, '''
            package demo.handles;

            final class Handles {
                static Object lookup(Class<?> type) {
                    Object lookup = java.lang.invoke.MethodHandles.lookup();
                    return lookup == null ? null : type.getDeclaredFields();
                }
            }
        '''

        when:
        fails 'compileJava'

        then:
        reportsAt HANDLES, 'getDeclaredFields'
        !reportsAt(HANDLES, 'MethodHandles.lookup')
    }

    void "forbidding all reflection honours no suppression, and still allows the classes named"() {
        given:
        groovyProject """
            forbidAll()
            allowIn 'demo.ReflectionAccess'
        """
        javaFile REFLECTION_ACCESS, '''
            package demo;

            final class ReflectionAccess {
                static Object methods(Class<?> type) {
                    return type.getDeclaredMethods();
                }
            }
        '''
        mainSubject '''
            Object members(Class<?> type) {
                @SuppressWarnings("NoReflection")
                Object members = type.getDeclaredFields();
                return members;
            }
        '''

        when:
        fails 'compileJava'

        then:
        reportsAt SUBJECT, 'getDeclaredFields'
        !reportsAt(REFLECTION_ACCESS, 'getDeclaredMethods')
    }

    void "forbidding all reflection and allowing some throughout the project is refused"() {
        given:
        groovyProject """
            forbidAll()
            allow 'CLASS_MEMBERS'
        """
        mainSubject 'Object nothing() { return null; }'

        when:
        fails 'compileJava'

        then:
        reported 'noReflection: forbidAll() allows no reflection outside the classes and packages named with allowIn(...), but [CLASS_MEMBERS] were allowed throughout the project'
    }

    void "a project can honour no suppression and still allow categories"() {
        given:
        groovyProject """
            suppressible = false
            allow 'CLASS_NAMES'
        """
        mainSubject '''
            Object members(Class<?> type) {
                String name = type.getSimpleName();
                @SuppressWarnings("NoReflection")
                Object members = type.getDeclaredMethods();
                return name.isEmpty() ? null : members;
            }
        '''

        when:
        fails 'compileJava'

        then:
        reportsAt SUBJECT, 'getDeclaredMethods'
        !reportsAt(SUBJECT, 'getSimpleName')
    }

    void "reflection can be reported as a warning"() {
        given:
        groovyProject "severity 'WARN'"
        mainSubject '''
            Object members(Class<?> type) {
                return type.getDeclaredMethods();
            }
        '''

        when:
        run 'compileJava'

        then:
        reported 'warning: [NoReflection]'
    }

    void "an unknown category, a malformed call and an unknown severity are refused, saying why"() {
        given:
        groovyProject configuration

        when:
        fails 'help'

        then:
        reported message

        where:
        configuration                             | message
        "allow 'CLASS_MEMBER'"                    | "noReflection: Unknown reflection category 'CLASS_MEMBER'. The categories are [ANNOTATION_SYNTHESIS, "
        "allowIn 'demo.Handles', 'HANDLE'"        | "noReflection: Unknown reflection category 'HANDLE'."
        "allowCalls 'java.lang.Class#get Simple'" | "noReflection: Invalid call pattern 'java.lang.Class#get Simple': 'get Simple' is not the name of a method"
        "allowIn 'demo handles'"                  | "noReflection: Invalid scope 'demo handles'"
        "severity 'INFO'"                         | "noReflection: the severity is ERROR or WARN, not 'INFO'."
    }

    void "test sources are checked only when asked to be"() {
        given:
        groovyProject()
        javaFile 'src/test/java/demo/SubjectTest.java', '''
            package demo;

            class SubjectTest {
                Object members(Class<?> type) {
                    return type.getDeclaredMethods();
                }
            }
        '''

        when:
        run 'compileTestJava'

        then:
        noExceptionThrown()

        when:
        groovyProject 'checkTests = true'
        fails 'compileTestJava'

        then:
        reportsAt 'src/test/java/demo/SubjectTest.java', 'getDeclaredMethods'
    }

    void "the source sets checked are the ones named"() {
        given:
        groovyProject('', 'sourceSets { tools }')
        javaFile 'src/tools/java/demo/Tool.java', '''
            package demo;

            class Tool {
                Object members(Class<?> type) {
                    return type.getDeclaredMethods();
                }
            }
        '''

        when:
        run 'compileToolsJava'

        then:
        noExceptionThrown()

        when:
        groovyProject("checkedSourceSets = ['main', 'tools']", 'sourceSets { tools }')
        fails 'compileToolsJava'

        then:
        reportsAt 'src/tools/java/demo/Tool.java', 'getDeclaredMethods'
    }

    void "can be configured from the Kotlin DSL"() {
        given:
        file('settings.gradle.kts').text = 'rootProject.name = "demo"'
        file('build.gradle.kts').text = """
            plugins {
                java
                id("io.micronaut.errorprone.no-reflection")
            }

            repositories {
                mavenCentral()
            }

            configurations.errorprone {
                withDependencies {
                    removeIf { it.group == "io.micronaut.errorprone" && it.name == "micronaut-errorprone-no-reflection" }
                }
            }

            dependencies {
                errorprone(files("${CHECKS}"))
            }

            noReflection {
                allow("CLASS_MEMBERS")
                allowCalls("java.lang.Class#getSimpleName")
                allowIn("demo.Subject", "HANDLES")
                forbid("demo.Legacy#*")
                suppressible = false
                severity("ERROR")
                checkedSourceSets = setOf("main")
                checkTests = true
                errorProneVersion = "${ERROR_PRONE_VERSION}"
            }
        """
        mainSubject '''
            Object members(Class<?> type) {
                String name = type.getSimpleName();
                Object lookup = java.lang.invoke.MethodHandles.lookup();
                return name.isEmpty() || lookup == null ? null : type.getDeclaredMethods();
            }
        '''

        when:
        run 'compileJava'

        then:
        noExceptionThrown()
    }

    void "is compatible with the configuration cache"() {
        given:
        groovyProject """
            allow 'CLASS_MEMBERS'
            allowCalls 'java.lang.Class#getSimpleName'
            allowIn 'demo.Subject', 'HANDLES'
            suppressible = false
        """
        mainSubject '''
            Object members(Class<?> type) {
                String name = type.getSimpleName();
                return name.isEmpty() ? null : type.getDeclaredMethods();
            }
        '''

        when:
        run 'compileJava', '--configuration-cache'
        run 'compileJava', '--configuration-cache'

        then:
        reported 'Reusing configuration cache.'
    }

    private static final String SUBJECT = 'src/main/java/demo/Subject.java'
    private static final String REFLECTION_ACCESS = 'src/main/java/demo/ReflectionAccess.java'
    private static final String HANDLES = 'src/main/java/demo/handles/Handles.java'

    private static String verifyDependencies(String configuration, String... expected) {
        """
            plugins {
                id 'java'
                id 'io.micronaut.errorprone.no-reflection'
            }

            noReflection {
                ${configuration}
            }

            tasks.register('verifyDependencies') {
                doLast {
                    def declared = configurations.errorprone.dependencies.collect { "\${it.group}:\${it.name}:\${it.version}".toString() } as Set
                    ${expected.collect { "assert declared.contains('$it')" }.join('\n                    ')}
                }
            }
        """
    }

    private void groovyProject(String configuration = '', String extraBuild = '') {
        file('settings.gradle').text = "rootProject.name = 'demo'"
        file('build.gradle').text = """
            plugins {
                id 'java'
                id 'io.micronaut.errorprone.no-reflection'
            }

            repositories {
                mavenCentral()
            }

            ${extraBuild}

            configurations.errorprone.withDependencies { dependencies ->
                dependencies.removeIf { it.group == 'io.micronaut.errorprone' && it.name == 'micronaut-errorprone-no-reflection' }
            }

            dependencies {
                errorprone files('${CHECKS}')
            }

            noReflection {
                ${configuration}
            }
        """
    }

    private void mainSubject(String members) {
        javaFile SUBJECT, """
            package demo;

            class Subject {
                $members
            }
        """
    }

    private void javaFile(String path, String content) {
        file(path).text = content
    }

    private File file(String path) {
        File file = projectDir.resolve(path).toFile()
        file.parentFile.mkdirs()
        file
    }

    private void run(String... arguments) {
        result = runner(arguments).build()
    }

    private void fails(String... arguments) {
        result = runner(arguments).buildAndFail()
    }

    private GradleRunner runner(String... arguments) {
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments([*arguments, '-S', '--no-build-cache'])
            .forwardOutput()
    }

    private void reported(String text) {
        assert result.output.contains(text)
    }

    // whether the check reported a call on the line of the file that holds the snippet, whatever else is printed
    private boolean reportsAt(String path, String snippet) {
        int line = file(path).readLines().findIndexOf { it.contains(snippet) } + 1
        assert line > 0: "'$snippet' is not in $path"
        Pattern.compile(Pattern.quote(path) + ':' + line + ': (error|warning): \\[NoReflection\\]').matcher(result.output).find()
    }
}
