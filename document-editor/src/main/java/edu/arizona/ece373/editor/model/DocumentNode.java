package edu.arizona.ece373.editor.model;

import java.util.List;

/**
 * Common type for {@link Section} and {@link Paragraph} (Composite pattern), so every
 * operation works at any nesting depth.
 *
 * <p>TODO (week of Nov 9): add {@code writeTo(DocumentWriter w, int depth)} for the renderers.
 */
public abstract class DocumentNode {

    /**
     * Adds every paragraph under this node to {@code out}, in document order.
     *
     * @param out the list to append to
     */
    public abstract void collectParagraphs(List<Paragraph> out);

    /** @return a deep, independent copy of this node and everything under it */
    public abstract DocumentNode deepCopy();

    /**
     * Appends this node's lines of the {@code outline} listing.
     *
     * @param out where to write
     * @param parentPath path of the containing section, or null for the root
     * @param state numbering state shared across the walk
     */
    abstract void appendOutline(StringBuilder out, SectionPath parentPath, OutlineState state);

    /** @return this node as a Section, or null if it is not one */
    Section asSection() {
        return null;
    }

    /** Numbering state for one outline walk. */
    static final class OutlineState {
        /** Next paragraph number to print. */
        int nextParagraph = 1;
        /** Number of subsections seen so far in the section being walked. */
        int sectionIndex = 0;
    }
}
