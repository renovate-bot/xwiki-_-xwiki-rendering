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
package org.xwiki.rendering.internal.blocknote;

import java.util.Optional;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.blocknote.BlockNoteMacroConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Converts the warning macro calls to quote blocks, and back, in order to test the {@link BlockNoteMacroConverter}
 * support.
 *
 * @version $Id$
 */
@Component
@Named("warning")
@Singleton
public class TestMacroBlockNoteConverter implements BlockNoteMacroConverter
{
    private static final String TYPE = "type";

    private static final String PROPS = "props";

    private static final String CONTENT = "content";

    private static final String CALL = "call";

    private static final String PARAMETERS = "parameters";

    private static final String WARNING = "xwikiWarning";

    private static final String XWIKI_PARAMETERS = "xwikiParameters";

    @Override
    public String getBlockType()
    {
        return "quote";
    }

    @Override
    public Optional<ObjectNode> toBlock(ObjectNode macroBlock)
    {
        JsonNode call = macroBlock.path(PROPS).path(CALL);
        JsonNode content = getInlineContent(call.path(CONTENT));
        if (!"xwikiMacroBlock".equals(macroBlock.path(TYPE).asText()) || call.path(PARAMETERS).has("image")
            || content == null) {
            return Optional.empty();
        }

        ObjectNode quote = JsonNodeFactory.instance.objectNode();
        quote.put(TYPE, getBlockType());
        ObjectNode props = quote.putObject(PROPS);
        props.put(WARNING, true);
        props.set(XWIKI_PARAMETERS, call.path(PARAMETERS));
        quote.set(CONTENT, content);
        return Optional.of(quote);
    }

    private JsonNode getInlineContent(JsonNode content)
    {
        if (content.isTextual()) {
            return content;
        } else if (content.isArray() && content.size() == 1 && "paragraph".equals(content.get(0).path(TYPE).asText())) {
            // Editable macro content.
            return content.get(0).path(CONTENT);
        }
        return null;
    }

    @Override
    public Optional<ObjectNode> toMacro(ObjectNode block)
    {
        JsonNode props = block.path(PROPS);
        if (!props.path(WARNING).asBoolean()) {
            return Optional.empty();
        }

        ObjectNode macroBlock = JsonNodeFactory.instance.objectNode();
        macroBlock.put(TYPE, "xwikiMacroBlock");
        ObjectNode call = macroBlock.putObject(PROPS).putObject(CALL);
        call.put("name", "warning");
        JsonNode parameters = props.path(XWIKI_PARAMETERS);
        call.set(PARAMETERS, parameters.isObject() ? parameters : JsonNodeFactory.instance.objectNode());
        call.set(CONTENT, block.path(CONTENT));
        return Optional.of(macroBlock);
    }
}
