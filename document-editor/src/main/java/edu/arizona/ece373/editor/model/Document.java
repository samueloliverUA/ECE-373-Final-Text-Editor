package edu.arizona.ece373.editor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The single document: owns the untitled root {@link Section} and finds paragraphs and
 * sections.
 *
 * <p>TODO: listeners ({@code addListener}, {@code fireChanged}) once DocumentListener and
 * DocumentEvent land; {@code createMemento}/{@code restore} are Jack's (week of Nov 9).
 */
public final class Document {

    private final Section root = new Section(new FormattedText(""));

    /** @return the untitled root section */
    public Section root() {
        return root;
    }

    /** @return every paragraph in document order */
    public List<Paragraph> paragraphs() {
        List<Paragraph> out = new ArrayList<>();
        root.collectParagraphs(out);
        return out;
    }

    /**
     * @param n 1-based paragraph number in document order
     * @return that paragraph
     * @throws EditException if there is no such paragraph
     */
    public Paragraph paragraph(int n) {
        List<Paragraph> all = paragraphs();
        if (n < 1 || n > all.size()) {
            throw new EditException("No paragraph " + n
                    + " (the document has " + all.size() + ")");
        }
        return all.get(n - 1);
    }

    /**
     * @param p a paragraph in this document
     * @return its 1-based number, or -1 if it is not in the document
     */
    public int numberOf(Paragraph p) {
        List<Paragraph> all = paragraphs();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) == p) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Walks a path from the root.
     *
     * @param path the section path; null means the root
     * @return that section
     * @throws EditException ("No section 2.4") if any level is missing
     */
    public Section section(SectionPath path) {
        Section current = root;
        if (path == null) {
            return current;
        }
        for (int level = 0; level < path.depth(); level++) {
            List<Section> subs = current.subsections();
            int i = path.index(level);
            if (i > subs.size()) {
                throw new EditException("No section " + path);
            }
            current = subs.get(i - 1);
        }
        return current;
    }

    /**
     * @return the tree with section paths and paragraph numbers, one item per line, or
     *     {@code (empty document)}
     */
    public String outline() {
        if (root.children().isEmpty()) {
            return "(empty document)";
        }
        StringBuilder sb = new StringBuilder();
        root.appendChildrenOutline(sb, null, new DocumentNode.OutlineState());
        return sb.toString().stripTrailing();
    }
}
