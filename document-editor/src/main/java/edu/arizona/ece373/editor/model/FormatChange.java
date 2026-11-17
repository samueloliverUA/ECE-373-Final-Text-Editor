package edu.arizona.ece373.editor.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * A change to exactly one formatting attribute, such as {@code bold on} or {@code size 14}.
 * Immutable. Values are checked when the change is made, so an invalid one never reaches the
 * document:
 * <ul>
 *   <li>size must be {@value #MIN_SIZE} to {@value #MAX_SIZE};</li>
 *   <li>color must be one of {@link #COLOR_NAMES} or {@code #RRGGBB} (stored lower case);</li>
 *   <li>font must not be blank.</li>
 * </ul>
 * Invalid values throw {@link EditException}.
 *
 * <p>Satisfies 6.5.1, 6.1.5. Design doc Fig 2b; UC3 step 2.
 */
public final class FormatChange {

    /** The formatting attribute being changed. */
    public enum Attribute { BOLD, ITALIC, FONT, SIZE, COLOR }

    /** Smallest allowed point size. */
    public static final int MIN_SIZE = 6;
    /** Largest allowed point size. */
    public static final int MAX_SIZE = 72;
    /** Color names accepted besides {@code #RRGGBB}. */
    public static final Set<String> COLOR_NAMES = Set.of("black", "white", "gray", "red",
            "orange", "yellow", "green", "blue", "purple", "brown", "pink", "cyan", "magenta");

    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");

    private final Attribute attribute;
    private final boolean flag;
    private final String text;
    private final int size;

    private FormatChange(Attribute attribute, boolean flag, String text, int size) {
        this.attribute = attribute;
        this.flag = flag;
        this.text = text;
        this.size = size;
    }

    /** @param on bold on or off @return the change */
    public static FormatChange bold(boolean on) {
        return new FormatChange(Attribute.BOLD, on, null, 0);
    }

    /** @param on italic on or off @return the change */
    public static FormatChange italic(boolean on) {
        return new FormatChange(Attribute.ITALIC, on, null, 0);
    }

    /**
     * @param name font name
     * @return the change
     * @throws EditException if the name is blank
     */
    public static FormatChange font(String name) {
        if (name == null || name.isBlank()) {
            throw new EditException("font name must not be empty");
        }
        return new FormatChange(Attribute.FONT, false, name.strip(), 0);
    }

    /**
     * @param points point size
     * @return the change
     * @throws EditException if outside {@value #MIN_SIZE}-{@value #MAX_SIZE}
     */
    public static FormatChange size(int points) {
        if (points < MIN_SIZE || points > MAX_SIZE) {
            throw new EditException("size must be " + MIN_SIZE + "-" + MAX_SIZE
                    + ", got " + points);
        }
        return new FormatChange(Attribute.SIZE, false, null, points);
    }

    /**
     * @param color a name from {@link #COLOR_NAMES} or {@code #RRGGBB}, any case
     * @return the change
     * @throws EditException if the color is not recognised
     */
    public static FormatChange color(String color) {
        String c = color == null ? "" : color.strip().toLowerCase(Locale.ROOT);
        if (!COLOR_NAMES.contains(c) && !HEX.matcher(c).matches()) {
            throw new EditException("unknown color '" + color + "'; use #RRGGBB or one of "
                    + new TreeSet<>(COLOR_NAMES));
        }
        return new FormatChange(Attribute.COLOR, false, c, 0);
    }

    /**
     * Builds a change from the attribute and value words the user typed, as returned by
     * {@code CommandParser}: {@code {"bold", "on"}}, {@code {"size", "14"}}, etc.
     *
     * @param attrAndValue exactly two strings: attribute name and value
     * @return the change
     * @throws EditException if the attribute is unknown or the value is invalid
     */
    public static FormatChange parse(String[] attrAndValue) {
        if (attrAndValue == null || attrAndValue.length != 2) {
            throw new EditException("a format change needs an attribute and a value");
        }
        return parse(attrAndValue[0], attrAndValue[1]);
    }

    /**
     * @param attr {@code bold}, {@code italic}, {@code font}, {@code size} or {@code color}
     * @param value the new value ({@code on}/{@code off} for bold and italic)
     * @return the change
     * @throws EditException if the attribute is unknown or the value is invalid
     */
    public static FormatChange parse(String attr, String value) {
        String a = attr == null ? "" : attr.toLowerCase(Locale.ROOT);
        return switch (a) {
            case "bold" -> bold(toggle(a, value));
            case "italic" -> italic(toggle(a, value));
            case "font" -> font(value);
            case "size" -> size(number(value));
            case "color" -> color(value);
            default -> throw new EditException("unknown format attribute '" + attr
                    + "'; use bold, italic, font, size or color");
        };
    }

    /** @return which attribute this changes */
    public Attribute attribute() {
        return attribute;
    }

    /** @return the new on/off value (BOLD and ITALIC only) */
    public boolean flag() {
        return flag;
    }

    /** @return the new font name or color (FONT and COLOR only) */
    public String text() {
        return text;
    }

    /** @return the new point size (SIZE only) */
    public int size() {
        return size;
    }

    /** @return the change as the user types it, e.g. {@code bold on} or {@code size 14} */
    @Override
    public String toString() {
        return switch (attribute) {
            case BOLD -> "bold " + (flag ? "on" : "off");
            case ITALIC -> "italic " + (flag ? "on" : "off");
            case FONT -> "font \"" + text + "\"";
            case SIZE -> "size " + size;
            case COLOR -> "color " + text;
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FormatChange c && c.attribute == attribute && c.flag == flag
                && c.size == size && Objects.equals(c.text, text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, flag, text, size);
    }

    private static boolean toggle(String attr, String value) {
        if ("on".equals(value)) {
            return true;
        }
        if ("off".equals(value)) {
            return false;
        }
        throw new EditException(attr + " must be on or off, got '" + value + "'");
    }

    private static int number(String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.strip());
        } catch (NumberFormatException e) {
            throw new EditException("size must be a whole number, got '" + value + "'");
        }
    }
}
