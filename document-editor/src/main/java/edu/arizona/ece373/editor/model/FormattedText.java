package edu.arizona.ece373.editor.model;

/**
 * A string where different characters carry different formatting.
 *
 * <p><b>Week 1 version:</b> holds plain text only. The public API ({@link #length()},
 * {@link #plainText()}) matches the class diagram, so callers will not change when the
 * {@code List<TextRun>} storage, insert/cut/splitAt/append and applyFormat are added.
 */
public final class FormattedText {

    private final StringBuilder text;

    /**
     * @param plain the initial text, with default formatting
     */
    public FormattedText(String plain) {
        this.text = new StringBuilder(plain == null ? "" : plain);
    }

    /** @return the number of characters */
    public int length() {
        return text.length();
    }

    /** @return the characters without formatting */
    public String plainText() {
        return text.toString();
    }

    /** @return an independent copy */
    public FormattedText copy() {
        return new FormattedText(text.toString());
    }

    @Override
    public String toString() {
        return plainText();
    }
}
