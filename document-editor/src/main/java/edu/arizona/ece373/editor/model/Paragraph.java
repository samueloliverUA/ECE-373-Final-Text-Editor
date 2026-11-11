package edu.arizona.ece373.editor.model;

import java.util.List;

/** A leaf node holding one block of formatted text. */
public final class Paragraph extends DocumentNode {

    private static final int OUTLINE_PREVIEW = 40;

    private final FormattedText text;

    /**
     * @param text the paragraph text
     */
    public Paragraph(FormattedText text) {
        this.text = text;
    }

    /** @return the paragraph's text */
    public FormattedText text() {
        return text;
    }

    @Override
    public void collectParagraphs(List<Paragraph> out) {
        out.add(this);
    }

    @Override
    public Paragraph deepCopy() {
        return new Paragraph(text.copy());
    }

    @Override
    void appendOutline(StringBuilder out, SectionPath parentPath, OutlineState state) {
        int depth = parentPath == null ? 0 : parentPath.depth();
        String plain = text.plainText();
        String preview = plain.length() <= OUTLINE_PREVIEW
                ? plain : plain.substring(0, OUTLINE_PREVIEW) + "...";
        out.append("  ".repeat(depth))
           .append('p').append(state.nextParagraph++)
           .append(": \"").append(preview).append("\"\n");
    }
}
