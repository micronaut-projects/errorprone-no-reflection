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
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The guide lists what is reported, and has to go on listing exactly that.
 */
class DocumentationTest {

    private static final Path CATEGORIES = Path.of("..", "src", "main", "docs", "guide", "categories.adoc");

    /** A call pattern in the guide, written in backticks with its | escaped for the table it sits in. */
    private static final Pattern DOCUMENTED = Pattern.compile("`([^`\\s]+#[^`\\s]*)`");

    @Test
    void theGuideListsEveryCallOfEveryCategoryAndNoOther() throws IOException {
        String guide = Files.readString(CATEGORIES);
        Set<String> documented = new TreeSet<>();
        Matcher matcher = DOCUMENTED.matcher(guide);
        while (matcher.find()) {
            documented.add(matcher.group(1).replace("\\|", "|"));
        }
        Set<String> reported = new TreeSet<>();
        ReflectionMatchers.patterns().forEach((category, patterns) -> {
            assertTrue(guide.contains("\n== " + category.name() + "\n"), "The guide has no section for " + category);
            reported.addAll(patterns);
        });
        assertEquals(reported, documented);
    }
}
