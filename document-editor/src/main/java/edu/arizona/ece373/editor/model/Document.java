package edu.arizona.ece373.editor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The single document: owns the untitled root {@link Section}, finds paragraphs and
 * sections, notifies {@link DocumentListener}s (Observer subject), and saves and restores
 * itself through {@link DocumentMemento}s (Memento originator).
 */
public final class Document {

    /** Creates an empty document: a root section with no children. */
    public Document() {
    }

    private Section root = new Section(new FormattedText(""));
    private final List<DocumentListener> listeners = new ArrayList<>();

    /**
     * Registers a listener. Listeners are called in the order they were added. Listeners
     * belong to the document, not its content, so they stay registered across
     * {@link #restore}.
     *
     * @param l the listener; not null
     */
    public void addListener(DocumentListener l) {
        listeners.add(Objects.requireNonNull(l, "listener"));
    }

    /**
     * Tells every listener, in registration order, that the document changed. Every
     * {@code EditCommand} calls this at the end of execute and undo.
     *
     * @param e what happened
     */
    public void fireChanged(DocumentEvent e) {
        for (DocumentListener l : List.copyOf(listeners)) {
            l.documentChanged(e);
        }
    }

    /**
     * Saves the whole document.
     *
     * @return an independent copy; later edits to this document never change it
     */
    public DocumentMemento createMemento() {
        return new DocumentMemento(root.deepCopy());
    }

    /**
     * Replaces the whole content with a saved copy. The memento itself is not consumed or
     * changed, so it can be restored again later. Does not notify listeners; the command
     * that calls this fires a {@link DocumentEvent.Kind#RESTORE} event.
     *
     * <p>Nodes obtained before a restore are no longer part of the document afterwards, so
     * commands must look nodes up again (by path or number) rather than keep references
     * across one.
     *
     * @param m a memento from {@link #createMemento}; not null
     */
    public void restore(DocumentMemento m) {
        root = Objects.requireNonNull(m, "memento").snapshot().deepCopy();
    }

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
