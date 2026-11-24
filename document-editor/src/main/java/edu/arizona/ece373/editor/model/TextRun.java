package edu.arizona.ece373.editor.model;

import java.util.Objects;

/**
 * A piece of text that has one formatting value throughout. Immutable: {@link FormattedText}
 * replaces runs rather than changing them.
 *
 * <p>Design doc Fig 2b, Section 5.6.
 */
public final class TextRun {

    /** The characters of this run. */
    public final String text;
    /** The formatting every character in this run has. */
    public final CharFormat format;

    /**
     * Creates a run of text with one format.
     *
     * @param text the characters (may be empty; FormattedText never stores empty runs)
     * @param format their formatting
     * @throws NullPointerException if either argument is null
     */
    public TextRun(String text, CharFormat format) {
        this.text = Objects.requireNonNull(text, "text");
        this.format = Objects.requireNonNull(format, "format");
    }

    /**
     * Returns the number of characters.
     *
     * @return the number of characters
     */
    public int length() {
        return text.length();
    }

    /**
     * Creates a copy of this run with different text.
     *
     * @param newText replacement characters
     * @return a run with the same formatting and different text
     */
    public TextRun withText(String newText) {
        return new TextRun(newText, format);
    }

    /**
     * Creates a copy of this run with different formatting.
     *
     * @param newFormat replacement formatting
     * @return a run with the same text and different formatting
     */
    public TextRun withFormat(CharFormat newFormat) {
        return new TextRun(text, newFormat);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextRun r && r.text.equals(text) && r.format.equals(format);
    }

    @Override
    public int hashCode() {
        return text.hashCode() * 31 + format.hashCode();
    }

    @Override
    public String toString() {
        return "\"" + text + "\"" + format;
    }
}
