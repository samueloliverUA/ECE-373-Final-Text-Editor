package edu.arizona.ece373.editor.model;

import java.util.Objects;

/**
 * The formatting of a character: bold, italic, font, size and color. An immutable value:
 * {@link #with(FormatChange)} returns a new object, and {@link #equals}/{@link #hashCode}
 * compare all five attributes so {@link FormattedText} can merge neighbouring runs.
 *
 * <p>Satisfies 6.5.1. Design doc Fig 2b, Section 5.6.
 */
public final class CharFormat {

    /** Font used when none has been set. */
    public static final String DEFAULT_FONT = "Times New Roman";
    /** Size used when none has been set. */
    public static final int DEFAULT_SIZE = 12;
    /** Color used when none has been set. */
    public static final String DEFAULT_COLOR = "black";

    /** Plain text: not bold, not italic, default font, size 12, black. */
    public static final CharFormat DEFAULT =
            new CharFormat(false, false, DEFAULT_FONT, DEFAULT_SIZE, DEFAULT_COLOR);

    /** True if bold. */
    public final boolean bold;
    /** True if italic. */
    public final boolean italic;
    /** Font name, e.g. {@code Arial}. */
    public final String font;
    /** Point size, 6 to 72. */
    public final int size;
    /** Color name (lower case) or {@code #rrggbb}. */
    public final String color;

    /**
     * @param bold bold on or off
     * @param italic italic on or off
     * @param font font name
     * @param size point size
     * @param color color name or {@code #rrggbb}
     */
    public CharFormat(boolean bold, boolean italic, String font, int size, String color) {
        this.bold = bold;
        this.italic = italic;
        this.font = Objects.requireNonNull(font, "font");
        this.size = size;
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * @param change the one attribute to change
     * @return a new format equal to this one except for that attribute
     */
    public CharFormat with(FormatChange change) {
        CharFormat made = switch (change.attribute()) {
            case BOLD -> new CharFormat(change.flag(), italic, font, size, color);
            case ITALIC -> new CharFormat(bold, change.flag(), font, size, color);
            case FONT -> new CharFormat(bold, italic, change.text(), size, color);
            case SIZE -> new CharFormat(bold, italic, font, change.size(), color);
            case COLOR -> new CharFormat(bold, italic, font, size, change.text());
        };
        return FormatPool.shared().intern(made);
    }

    /** @return true if every attribute matches {@link #DEFAULT} */
    public boolean isDefault() {
        return equals(DEFAULT);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CharFormat f
                && f.bold == bold && f.italic == italic && f.size == size
                && f.font.equals(font) && f.color.equals(color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, font, size, color);
    }

    @Override
    public String toString() {
        return "[" + (bold ? "bold " : "") + (italic ? "italic " : "")
                + font + " " + size + " " + color + "]";
    }
}
