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
package org.xwiki.rendering.blocknote;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Converts the calls of a macro to a dedicated BlockNote block, and back. The component hint is the id of the macro
 * whose calls are converted. Without a converter, macro calls are represented in BlockNote with generic macro blocks:
 *
 * <pre>
 * {@code
 * {
 *   "type": "xwikiMacroBlock", // "xwikiInlineMacro" for inline macro calls
 *   "props": {
 *     "call": {
 *       "name": "code",
 *       "parameters": {"language": "java"},
 *       "content": "public class Test {}"
 *     },
 *     "output": [...]
 *   }
 * }
 * }
 * </pre>
 *
 * The values of the macro call parameters and content are either strings, or BlockNote JSON (an array of blocks or of
 * inline content) when the macro marks them as editable in-place (non-generated content). The macro output is a list
 * of BlockNote blocks or inline content (depending on whether the macro call is inline or not), and it's empty when
 * the macro was not executed.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Role
@Unstable
public interface BlockNoteMacroConverter
{
    /**
     * @return the type of the BlockNote blocks produced by this converter (e.g. "codeBlock"); used to find the
     *         converters that can convert a BlockNote block back to a macro call
     */
    String getBlockType();

    /**
     * Converts a macro call to a dedicated BlockNote block.
     *
     * @param macroBlock the generic BlockNote macro block (see the class documentation for its structure)
     * @return the BlockNote block that replaces the given macro block, or an empty optional if the given macro call is
     *         not supported by this converter, in which case the generic macro block is kept
     */
    Optional<ObjectNode> toBlock(ObjectNode macroBlock);

    /**
     * Converts a BlockNote block to a macro call.
     *
     * @param block a BlockNote block whose type is {@link #getBlockType()}
     * @return the generic BlockNote macro block (see the class documentation for its structure; the output is not
     *         needed) that is used to save the given block, or an empty optional if the given block is not supported by
     *         this converter, in which case the block is saved as usual
     */
    Optional<ObjectNode> toMacro(ObjectNode block);
}
