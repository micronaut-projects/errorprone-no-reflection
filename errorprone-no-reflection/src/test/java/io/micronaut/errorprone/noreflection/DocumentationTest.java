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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The guide lists what each category reports, and has to go on listing exactly that, under the right category.
 */
class DocumentationTest {

    private static final Path CATEGORIES = Path.of("..", "src", "main", "docs", "guide", "categories.adoc");

    /** A call pattern in the guide, written in backticks with its | escaped for the table it sits in. */
    private static final Pattern DOCUMENTED = Pattern.compile("`([^`\\s]+#[^`\\s]*)`");

    @Test
    void theGuideListsTheCallsOfEachCategoryUnderIt() throws IOException {
        String guide = Files.readString(CATEGORIES);
        Map<ReflectionCategory, Set<String>> documented = new EnumMap<>(ReflectionCategory.class);
        String[] sections = guide.split("\n== ");
        // what precedes the first heading introduces the categories, and names none of them
        for (int i = 1; i < sections.length; i++) {
            String section = sections[i];
            ReflectionCategory category = ReflectionCategory.named(section.substring(0, section.indexOf('\n')));
            Set<String> patterns = new TreeSet<>();
            Matcher matcher = DOCUMENTED.matcher(section);
            while (matcher.find()) {
                patterns.add(matcher.group(1).replace("\\|", "|"));
            }
            if (!patterns.isEmpty()) {
                documented.put(category, patterns);
            }
        }
        Map<ReflectionCategory, Set<String>> reported = new EnumMap<>(ReflectionCategory.class);
        ReflectionMatchers.patterns().forEach((category, patterns) -> reported.put(category, new TreeSet<>(patterns)));
        assertEquals(reported, documented);
    }
}
