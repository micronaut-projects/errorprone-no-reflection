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

import java.util.Locale;

/**
 * Why a call is reported: what the platform does when it is called, and which cache it fills. Each problem belongs to a
 * {@link ReflectionCategory} and has a section of its own in the guide, which a report links to.
 *
 * <p>A problem groups the calls that reach for reflection the same way - {@code getDeclaredMethods} and
 * {@code getDeclaredMethod} fill the same declared methods of a class, {@code getSimpleName} another entry of its
 * reflection data.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.2
 */
enum ReflectionProblem {

    ANNOTATION_SYNTHESIS(ReflectionCategory.ANNOTATION_SYNTHESIS),

    TARGET_METHOD(ReflectionCategory.TARGET_MEMBERS),
    INJECTION_POINT_FIELD(ReflectionCategory.TARGET_MEMBERS),

    REFLECTION_UTILS(ReflectionCategory.REFLECTION_UTILS),

    BEAN_INFO(ReflectionCategory.BEANS),
    BEANS_INSTANTIATE(ReflectionCategory.BEANS),
    BEAN_STATEMENT(ReflectionCategory.BEANS),
    FEATURE_DESCRIPTOR(ReflectionCategory.BEANS),
    PROPERTY_DESCRIPTOR(ReflectionCategory.BEANS),
    METHOD_DESCRIPTOR(ReflectionCategory.BEANS),
    EVENT_SET_DESCRIPTOR(ReflectionCategory.BEANS),
    EVENT_HANDLER(ReflectionCategory.BEANS),
    BEAN_ENCODER(ReflectionCategory.BEANS),
    PERSISTENCE_DELEGATE(ReflectionCategory.BEANS),
    XML_DECODER(ReflectionCategory.BEANS),
    PROPERTY_EDITOR(ReflectionCategory.BEANS),

    OBJECT_INPUT(ReflectionCategory.SERIALIZATION),
    OBJECT_OUTPUT(ReflectionCategory.SERIALIZATION),
    OBJECT_STREAM_CLASS(ReflectionCategory.SERIALIZATION),

    UNSAFE(ReflectionCategory.UNSAFE),
    REFLECTION_FACTORY(ReflectionCategory.UNSAFE),

    INSTRUMENTATION(ReflectionCategory.INSTRUMENTATION),

    PROXY(ReflectionCategory.PROXY),
    INVOKE_DEFAULT(ReflectionCategory.PROXY),
    METHOD_HANDLE_PROXIES(ReflectionCategory.PROXY),

    METHOD_HANDLES(ReflectionCategory.HANDLES),
    OBJECT_METHODS(ReflectionCategory.HANDLES),
    SWITCH_BOOTSTRAPS(ReflectionCategory.HANDLES),
    CONSTANT_RESOLUTION(ReflectionCategory.HANDLES),
    FOREIGN_LINKER(ReflectionCategory.HANDLES),

    SERVICE_LOADER(ReflectionCategory.SERVICE_LOADING),
    SERVICE_PROVIDER(ReflectionCategory.SERVICE_LOADING),
    SOFT_SERVICE_LOADER(ReflectionCategory.SERVICE_LOADING),

    CLASS_FOR_NAME(ReflectionCategory.CLASS_LOADING),
    CLASS_LOADER(ReflectionCategory.CLASS_LOADING),
    MODULE_LAYER(ReflectionCategory.CLASS_LOADING),
    RESOURCE_BUNDLE(ReflectionCategory.CLASS_LOADING),
    CLASS_UTILS_FOR_NAME(ReflectionCategory.CLASS_LOADING),

    FIELD_UPDATER(ReflectionCategory.FIELD_UPDATERS),

    ENUM_CONSTANTS(ReflectionCategory.ENUM_CONSTANTS),
    ENUM_VALUE_OF(ReflectionCategory.ENUM_CONSTANTS),
    ENUM_DESCRIPTION(ReflectionCategory.ENUM_CONSTANTS),
    ENUM_SET(ReflectionCategory.ENUM_CONSTANTS),
    ENUM_MAP(ReflectionCategory.ENUM_CONSTANTS),

    SIMPLE_NAME(ReflectionCategory.CLASS_NAMES),
    CANONICAL_NAME(ReflectionCategory.CLASS_NAMES),

    CLASS_INTERFACES(ReflectionCategory.INTERFACES),
    CLASS_HIERARCHY(ReflectionCategory.INTERFACES),

    CLASS_GENERIC_SIGNATURE(ReflectionCategory.GENERIC_SIGNATURES),
    MEMBER_GENERIC_SIGNATURE(ReflectionCategory.GENERIC_SIGNATURES),
    PARAMETERIZED_TYPE(ReflectionCategory.GENERIC_SIGNATURES),
    GENERIC_TYPES(ReflectionCategory.GENERIC_SIGNATURES),
    GENERIC_TYPE_UTILS(ReflectionCategory.GENERIC_SIGNATURES),

    DECLARED_ANNOTATIONS(ReflectionCategory.ANNOTATIONS),
    PARAMETER_ANNOTATIONS(ReflectionCategory.ANNOTATIONS),
    ANNOTATION_DEFAULT(ReflectionCategory.ANNOTATIONS),
    ANNOTATED_TYPES(ReflectionCategory.ANNOTATIONS),

    PUBLIC_METHODS(ReflectionCategory.CLASS_MEMBERS),
    DECLARED_METHODS(ReflectionCategory.CLASS_MEMBERS),
    PUBLIC_CONSTRUCTORS(ReflectionCategory.CLASS_MEMBERS),
    DECLARED_CONSTRUCTORS(ReflectionCategory.CLASS_MEMBERS),
    PUBLIC_FIELDS(ReflectionCategory.CLASS_MEMBERS),
    DECLARED_FIELDS(ReflectionCategory.CLASS_MEMBERS),
    RECORD_COMPONENTS(ReflectionCategory.CLASS_MEMBERS),
    PERMITTED_SUBCLASSES(ReflectionCategory.CLASS_MEMBERS),
    NEST_MEMBERS(ReflectionCategory.CLASS_MEMBERS),
    NESTED_CLASSES(ReflectionCategory.CLASS_MEMBERS),
    ENCLOSING_MEMBER(ReflectionCategory.CLASS_MEMBERS),
    EXECUTABLE_PARAMETERS(ReflectionCategory.CLASS_MEMBERS),

    ACCESSIBILITY(ReflectionCategory.REFLECTIVE_ACCESS),
    CONSTRUCTOR_ACCESSOR(ReflectionCategory.REFLECTIVE_ACCESS),
    METHOD_ACCESSOR(ReflectionCategory.REFLECTIVE_ACCESS),
    FIELD_ACCESSOR(ReflectionCategory.REFLECTIVE_ACCESS),
    ARRAYS(ReflectionCategory.REFLECTIVE_ACCESS),
    CLASS_NEW_INSTANCE(ReflectionCategory.REFLECTIVE_ACCESS),
    OPEN_MODULE(ReflectionCategory.REFLECTIVE_ACCESS),
    INSTANTIATION_UTILS(ReflectionCategory.REFLECTIVE_ACCESS),

    CUSTOM(ReflectionCategory.CUSTOM);

    /** The guide of the release the check was built from is published under {@code latest} once it is out. */
    static final String GUIDE = "https://micronaut-projects.github.io/errorprone-no-reflection/latest/guide/";

    private final ReflectionCategory category;

    ReflectionProblem(ReflectionCategory category) {
        this.category = category;
    }

    /**
     * @return The category the problem belongs to, which is what a build allows
     */
    ReflectionCategory category() {
        return category;
    }

    /**
     * @return The id of the section of the guide describing the problem, such as {@code problem-simple-name}
     */
    String anchor() {
        return "problem-" + name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * @return The address of the section of the guide describing the problem
     */
    String link() {
        return GUIDE + "#" + anchor();
    }
}
