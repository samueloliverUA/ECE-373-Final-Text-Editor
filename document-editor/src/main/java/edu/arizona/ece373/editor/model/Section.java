package edu.arizona.ece373.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A titled container of paragraphs and subsections, to any depth. */
public final class Section extends DocumentNode {

    private final FormattedText title;
    private final List<DocumentNode> children = new ArrayList<>();

    /**
     * @param title the section title (empty for the document root)
     */
    public Section(FormattedText title) {
        this.title = title;
    }

    /** @return the title */
    public FormattedText title() {
        return title;
    }

    /** @return the children in order, read-only */
    public List<DocumentNode> children() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Inserts a child.
     *
     * @param index position among the children, 0 to {@code children().size()}
     * @param n the node to insert
     */
    public void add(int index, DocumentNode n) {
        children.add(index, n);
    }

    /**
     * Appends a child at the end.
     *
     * @param n the node to append
     */
    public void add(DocumentNode n) {
        children.add(n);
    }

    /**
     * Removes a child, compared by identity.
     *
     * @param n the node to remove
     */
    public void remove(DocumentNode n) {
        int i = indexOf(n);
        if (i >= 0) {
            children.remove(i);
        }
    }

    /**
     * @param n a child node
     * @return its index among the children (by identity), or -1
     */
    public int indexOf(DocumentNode n) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == n) {
                return i;
            }
        }
        return -1;
    }

    /** @return the subsections only, in order */
    public List<Section> subsections() {
        List<Section> out = new ArrayList<>();
        for (DocumentNode child : children) {
            Section s = child.asSection();
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    @Override
    public void collectParagraphs(List<Paragraph> out) {
        for (DocumentNode child : children) {
            child.collectParagraphs(out);
        }
    }

    /** Writes the heading at {@code depth}, then each child at {@code depth + 1}. */
    @Override
    public void writeTo(DocumentWriter w, int depth) {
        w.heading(title, depth);
        for (DocumentNode child : children) {
            child.writeTo(w, depth + 1);
        }
    }

    @Override
    public Section deepCopy() {
        Section copy = new Section(title.copy());
        for (DocumentNode child : children) {
            copy.children.add(child.deepCopy());
        }
        return copy;
    }

    @Override
    void appendOutline(StringBuilder out, SectionPath parentPath, OutlineState state) {
        SectionPath myPath = SectionPath.child(parentPath, ++state.sectionIndex);
        out.append("  ".repeat(myPath.depth() - 1))
           .append(myPath).append(' ').append(title.plainText()).append('\n');
        appendChildrenOutline(out, myPath, state);
    }

    /** Writes the children's outline lines, numbering subsections from 1. */
    void appendChildrenOutline(StringBuilder out, SectionPath myPath, OutlineState state) {
        int saved = state.sectionIndex;
        state.sectionIndex = 0;
        for (DocumentNode child : children) {
            child.appendOutline(out, myPath, state);
        }
        state.sectionIndex = saved;
    }

    @Override
    Section asSection() {
        return this;
    }
}
