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

    /**
     * Removes the characters from start (inclusive) to end (exclusive) and returns them,
     * so an undo can put them back.
     *
     * @param start first character to remove
     * @param end one past the last character to remove
     * @return the removed piece
     * @throws EditException if 0 <= start <= end <= length() does not hold
     */
    public FormattedText cut(int start, int end) {
        if (start < 0 || start > end || end > text.length()) {
            throw new EditException("Bad range " + start + "-" + end
                    + " for text of " + text.length() + " characters");
        }
        FormattedText removed = new FormattedText(text.substring(start, end));
        text.delete(start, end);
        return removed;
    }

    /**
     * Inserts another piece of text at an offset, keeping that piece's formatting.
     *
     * @param offset where to insert; 0 is the start, length() is the end
     * @param piece the text to insert
     * @throws EditException if offset is outside 0..length()
     */
    public void insert(int offset, FormattedText piece) {
        if (offset < 0 || offset > text.length()) {
            throw new EditException("Bad offset " + offset
                    + " for text of " + text.length() + " characters");
        }
        text.insert(offset, piece.plainText());
    }

    /**
     * Adds another piece of text to the end, keeping that piece's formatting.
     * The piece itself is not changed.
     *
     * @param piece the text to add
     */
    public void append(FormattedText piece) {
        text.append(piece.plainText());
    }

    /**
     * Keeps the characters before offset and returns the rest as a new piece.
     *
     * @param offset where to split; 0 to length()
     * @return the characters from offset to the end
     * @throws EditException if offset is outside 0..length()
     */
    public FormattedText splitAt(int offset) {
        if (offset < 0 || offset > text.length()) {
            throw new EditException("Bad offset " + offset
                    + " for text of " + text.length() + " characters");
        }
        FormattedText right = new FormattedText(text.substring(offset));
        text.setLength(offset);
        return right;
    }

    @Override
    public String toString() {
        return plainText();
    }

}
