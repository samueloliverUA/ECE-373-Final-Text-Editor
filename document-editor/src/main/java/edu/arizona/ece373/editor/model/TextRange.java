package edu.arizona.ece373.editor.model;

/**
 * A character range inside one paragraph: paragraph number (1-based), start and end
 * (zero-based offsets, end exclusive). Immutable. A position is a range with
 * {@code start == end}.
 *
 * <p>Satisfies 6.1.5.
 */
public final class TextRange {

    /** 1-based paragraph number in document order. */
    public final int paragraph;
    /** Zero-based start offset (inclusive). */
    public final int start;
    /** Zero-based end offset (exclusive). */
    public final int end;

    /**
     * @param paragraph 1-based paragraph number
     * @param start start offset, inclusive
     * @param end end offset, exclusive
     */
    public TextRange(int paragraph, int start, int end) {
        this.paragraph = paragraph;
        this.start = start;
        this.end = end;
    }

    /**
     * Checks this range against the current document.
     *
     * @param d the document
     * @throws EditException if the paragraph is missing or the offsets do not fit
     */
    public void validate(Document d) {
        int length = d.paragraph(paragraph).text().length();
        if (start < 0 || start > end) {
            throw new EditException("start is after end in " + this);
        }
        if (end > length) {
            throw new EditException("p" + paragraph + " has " + length + " characters");
        }
    }

    /** @return true if this is a single position ({@code start == end}) */
    public boolean isPosition() {
        return start == end;
    }

    /** @return the same syntax the user types, e.g. {@code p2:5-12} */
    @Override
    public String toString() {
        return "p" + paragraph + ":" + start + "-" + end;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextRange r
                && r.paragraph == paragraph && r.start == start && r.end == end;
    }

    @Override
    public int hashCode() {
        return (paragraph * 31 + start) * 31 + end;
    }
}
