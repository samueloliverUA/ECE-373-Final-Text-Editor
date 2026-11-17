package edu.arizona.ece373.editor.view;

import edu.arizona.ece373.editor.model.Document;

/**
 * Turns the whole document into output text (Strategy pattern): {@link PlainTextRenderer}
 * shows words only, {@link MarkupRenderer} shows formatting markers. Rendering never changes
 * the document.
 *
 * <p>Satisfies 6.6.1-6.6.3, 6.9.2. Design doc Fig 2b; Section 5.4.
 */
public interface Renderer {

    /**
     * @param d the document to show
     * @param b how to break paragraphs into lines
     * @param width the maximum line length
     * @return the rendered text, lines separated by {@code \n}, with no trailing newline
     */
    String render(Document d, LineBreaker b, int width);
}
