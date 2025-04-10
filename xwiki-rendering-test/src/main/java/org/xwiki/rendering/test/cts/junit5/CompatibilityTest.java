/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.test.cts.junit5;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.xwiki.rendering.test.cts.Scope;
import org.xwiki.rendering.test.cts.Syntax;
import org.xwiki.rendering.test.cts.TestData;
import org.xwiki.rendering.test.integration.AbstractRenderingTest;

/**
 * Run all tests found in resources files located in the classpath, for a given Syntax.
 * <p>
 * The algorithm is the following:
 * <ul>
 *   <li>Look for {@code cts/[scope]} resources in the classpath where {@code [scope]} represents the value of the
 *       {@code &#064;Scope} annotation prefixed by {@code cts\\.}. By default, if no Scope annotation is defined,
 *       {@code .*\\.xml} is used, leading to a total regex of {@code cts\\..*\\.xml}. This is the regex that's used
 *       to look for resources in the classpath. For example the following test file would match:
 *       {@code cts/simple/bold/bold1.inout.xml}. We call these {@code CTS} resources.</li>
 *   <li>For each {@code CTS} resource found look for equivalent test input and output files for the tested Syntax.
 *       For example if we have {@code cts/simple/bold/bold1.inout.xml} then if the Syntax is {@code xwiki/2.0} look
 *       for {@code xwiki20/simple/bold/bold1.[in|out|inout].txt} test files. We call them {@code SYN} resources.
 *   </li>
 *   <li>For each {@code SYN IN} resource, parse it with the corresponding Syntax parser and render the generated XDOM
 *       with the CTS Renderer, and compare the results with the {@code CTS OUT} resource. Note that if no
 *       {@code SYN IN} resource is found generate a warning in the test logs.</li>
 *   <li>For each {@code SYN OUT} resource, parse the {@code CTS IN} resource with the CTS Syntax parser and render the
 *       generated XDOM with the Syntax Renderer, and compare the results with the {@code SYN OUT} resource.
 *       Note that if no {@code SYN OUT} resource is found generate a warning in the test logs.</li>
 * </ul>
 *
 * <p>Usage Example</p>
 * <pre><code>
 * &#064;AllComponents
 * &#064;Syntax("xwiki/2.0")
 * &#064;Scope("simple")
 * class XXXCompatibilityTest extends CompatibilityTest
 * {
 * }
 * </code></pre>
 * <p>
 * It's also possible to get access to the underlying Component Manager used, for example in order to register
 * Mock implementations of components. For example:
 * </p>
 * <pre><code>
 * &#064;AllComponents
 * &#064;Syntax("xwiki/2.0")
 * &#064;Scope("simple")
 * class XXXCompatibilityTest extends CompatibilityTest
 * {
 *     &#064;Initialized
 *     public void initialize(MockitoComponentManager componentManager)
 *     {
 *         // Init mocks here for example
 *     }
 * }
 * </code></pre>
 *
 * @version $Id$
 * @since 17.0.0RC1
 */
public class CompatibilityTest extends AbstractRenderingTest
{
    /**
     * @return the dynamic list of tests to execute
     */
    @TestFactory
    Stream<DynamicTest> renderingTests() throws Exception
    {
        // Step 1: Generate inputs

        // If a Scope Annotation is present then use it to define the scope
        Scope scopeAnnotation = getClass().getAnnotation(Scope.class);
        String packageFilter = "";
        String pattern = Scope.DEFAULT_PATTERN;
        if (scopeAnnotation != null) {
            packageFilter = scopeAnnotation.value();
            pattern = scopeAnnotation.pattern();
        }

        // Get the specified Syntax from the Syntax annotation
        Syntax syntaxAnnotation = getClass().getAnnotation(Syntax.class);
        if (syntaxAnnotation == null) {
            throw new RuntimeException("You must specify a Syntax using the @Syntax annotation");
        }
        String syntaxId = syntaxAnnotation.value();
        String metadataSyntaxId = syntaxAnnotation.metadata();
        if (StringUtils.isEmpty(metadataSyntaxId)) {
            metadataSyntaxId = syntaxId;
        }

        // Get all the data for the tests to execute
        TestDataGenerator generator = new TestDataGenerator(getComponentManager());
        List<TestData> testDataList = generator.generate(syntaxId, packageFilter, pattern);

        // Generate test names
        Function<TestData, String> displayNameGenerator = (input) ->
            String.format("%s [%s, %s:%s, CTS:%s]%s", input.prefix, input.syntaxId,
                input.isSyntaxInputTest ? "IN" : "OUT", input.syntaxExtension, input.ctsExtension,
                input.isIgnored() ? " - Missing" : "");

        // Generate tests to execute
        String finalMetadataSyntaxId = metadataSyntaxId;
        ThrowingConsumer<TestData> testExecutor = (input) -> {
            new InternalRenderingTest(input, finalMetadataSyntaxId, getComponentManager()).execute();
        };

        // Return the dynamically created tests
        return DynamicTest.stream(testDataList.iterator(), displayNameGenerator, testExecutor);
    }
}
