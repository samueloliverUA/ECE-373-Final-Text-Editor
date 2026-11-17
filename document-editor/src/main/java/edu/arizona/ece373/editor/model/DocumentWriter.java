package edu.arizona.ece373.editor.model;

/**
 * Callback the document tree writes itself into; renderers implement it.
 *
 * <p>The tree decides the ORDER: {@link DocumentNode#writeTo} visits every node in document
 * order, each section's heading before its children. The writer decides the LOOK: numbering,
 * wrapping and markup.
 *
 * <p>Levels: the untitled document root is level 0, top-level sections are level 1, their
 * subsections level 2, and so on. The root's heading is reported like any other, so a writer
 * should print nothing for level 0.
 *
 * <p>Satisfies 6.2.3, 6.6.1, 6.6.2. Design doc Fig 2b, Fig 6.
 */
public interface DocumentWriter {

    /**
     * Called for each section, before anything inside it.
     *
     * @param t the section title (do not modify)
     * @param level 0 for the document root, 1 for top-level sections, and so on
     */
    void heading(FormattedText t, int level);

    /**
     * Called for each paragraph, in document order.
     *
     * @param t the paragraph text (do not modify)
     */
    void paragraph(FormattedText t);
}
