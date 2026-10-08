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
package org.xwiki.rendering.internal.parser.blocknote.blocks;

import java.util.Deque;
import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.parser.blocknote.Context;
import org.xwiki.rendering.parser.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Code block parser.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@Component
@Named(CodeBlockParser.CODE)
@Singleton
public class CodeBlockParser extends AbstractBlockParser
{
    /**
     * This component's role hint. Also the type of blocks handled by this parser.
     */
    public static final String CODE = "codeBlock";

    /**
     * The code block property that indicates the code language.
     */
    public static final String LANGUAGE = "language";

    /**
     * The parameter used on verbatim blocks to store the language of the verbatim content. This is mapped to the
     * language of the code block from BlockNote.
     */
    public static final String VERBATIM_LANGUAGE = "data-xwiki-verbatim-language";

    /**
     * The code block property that indicates whether the verbatim content starts with a new line that is not part of
     * the code block content. Browsers ignore the new line right after the start tag of a {@code pre} element, so the
     * canonical {@code {{{\ncode\n}}}} verbatim syntax is displayed without empty lines around the code. We do the same
     * in the code block, so that it looks the same in edit and view mode. Missing means {@code true}.
     *
     * @since 18.9.0RC1
     */
    public static final String LEADING_NEW_LINE = "xwikiLeadingNewLine";

    /**
     * The code block property that indicates whether the verbatim content ends with a new line that is not part of the
     * code block content. Browsers don't display an empty line for the new line at the end of a {@code pre} element.
     * Missing means {@code true}.
     *
     * @see #LEADING_NEW_LINE
     * @since 18.9.0RC1
     */
    public static final String TRAILING_NEW_LINE = "xwikiTrailingNewLine";

    /**
     * The code block property that indicates that the code block was produced from a verbatim block. Code blocks can
     * also be produced from macro calls (see {@link org.xwiki.rendering.blocknote.BlockNoteMacroConverter}), in which
     * case they are saved back as macro calls, unless this property is {@code true}. Missing means {@code false}.
     *
     * @since 18.9.0RC1
     */
    public static final String VERBATIM = "xwikiVerbatim";

    private static final String NEW_LINE = "\n";

    @Override
    public void parse(ObjectNode codeBlock, Deque<Context> contextStack) throws ParseException
    {
        Map<String, String> parameters = getBlockParameters(codeBlock);
        JsonNode language = codeBlock.path(PROPS).path(LANGUAGE);
        if (language.isTextual()) {
            parameters.put(VERBATIM_LANGUAGE, language.asText());
        }
        StringBuilder code = new StringBuilder();
        JsonNode properties = codeBlock.path(PROPS);
        if (properties.path(LEADING_NEW_LINE).asBoolean(true)) {
            code.append(NEW_LINE);
        }
        code.append(getTextContent(codeBlock));
        if (properties.path(TRAILING_NEW_LINE).asBoolean(true)) {
            code.append(NEW_LINE);
        }
        contextStack.peek().listener().onVerbatim(code.toString(), false, parameters);
    }
}
