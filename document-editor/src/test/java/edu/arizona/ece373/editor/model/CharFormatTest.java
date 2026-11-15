package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link CharFormat} and {@link FormatChange}: with() changes one attribute and returns
 * a new object; equals/hashCode; size and color validation.
 *
 * <p>Satisfies 6.5.1.
 */
class CharFormatTest {

    private static final CharFormat D = CharFormat.DEFAULT;

    @Nested
    class Defaults {

        @Test
        void defaultValues() {
            assertFalse(D.bold);
            assertFalse(D.italic);
            assertEquals(CharFormat.DEFAULT_FONT, D.font);
            assertEquals(12, D.size);
            assertEquals("black", D.color);
            assertTrue(D.isDefault());
        }
    }

    @Nested
    class With {

        @Test
        void boldChangesOnlyBold() {
            CharFormat f = D.with(FormatChange.bold(true));
            assertEquals(new CharFormat(true, false, D.font, D.size, D.color), f);
        }

        @Test
        void italicChangesOnlyItalic() {
            assertEquals(new CharFormat(false, true, D.font, D.size, D.color),
                    D.with(FormatChange.italic(true)));
        }

        @Test
        void fontChangesOnlyFont() {
            assertEquals(new CharFormat(false, false, "Arial", D.size, D.color),
                    D.with(FormatChange.font("Arial")));
        }

        @Test
        void sizeChangesOnlySize() {
            assertEquals(new CharFormat(false, false, D.font, 14, D.color),
                    D.with(FormatChange.size(14)));
        }

        @Test
        void colorChangesOnlyColor() {
            assertEquals(new CharFormat(false, false, D.font, D.size, "red"),
                    D.with(FormatChange.color("red")));
        }

        @Test
        void returnsNewObjectAndLeavesOriginal() {
            CharFormat f = D.with(FormatChange.bold(true));
            assertNotEquals(D, f);
            assertFalse(D.bold);
            assertFalse(D.with(FormatChange.bold(false)).bold);
        }

        @Test
        void changesStack() {
            CharFormat f = D.with(FormatChange.bold(true)).with(FormatChange.size(20))
                    .with(FormatChange.bold(false));
            assertEquals(D.with(FormatChange.size(20)), f);
        }
    }

    @Nested
    class Equality {

        @Test
        void equalWhenAllAttributesMatch() {
            CharFormat a = new CharFormat(true, true, "Arial", 14, "#ff0000");
            CharFormat b = new CharFormat(true, true, "Arial", 14, "#ff0000");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        void differsOnAnyAttribute() {
            CharFormat a = new CharFormat(true, true, "Arial", 14, "red");
            assertNotEquals(a, new CharFormat(false, true, "Arial", 14, "red"));
            assertNotEquals(a, new CharFormat(true, false, "Arial", 14, "red"));
            assertNotEquals(a, new CharFormat(true, true, "Courier", 14, "red"));
            assertNotEquals(a, new CharFormat(true, true, "Arial", 15, "red"));
            assertNotEquals(a, new CharFormat(true, true, "Arial", 14, "blue"));
            assertNotEquals(a, null);
        }
    }

    @Nested
    class Validation {

        @ParameterizedTest
        @ValueSource(ints = {6, 12, 72})
        void sizeInRangeAccepted(int size) {
            assertEquals(size, assertDoesNotThrow(() -> FormatChange.size(size)).size());
        }

        @ParameterizedTest
        @ValueSource(ints = {5, 73, 0, -1})
        void sizeOutOfRangeRejected(int size) {
            assertThrows(EditException.class, () -> FormatChange.size(size));
        }

        @ParameterizedTest
        @ValueSource(strings = {"red", "RED", "Blue", "#00ff00", "#ABCDEF"})
        void knownColorsAccepted(String color) {
            assertDoesNotThrow(() -> FormatChange.color(color));
        }

        @Test
        void colorsStoredLowerCase() {
            assertEquals("#abcdef", FormatChange.color("#ABCDEF").text());
            assertEquals("red", FormatChange.color("RED").text());
        }

        @ParameterizedTest
        @ValueSource(strings = {"reddish", "", "#12345", "#1234567", "#GGGGGG", "ff0000"})
        void badColorsRejected(String color) {
            assertThrows(EditException.class, () -> FormatChange.color(color));
        }

        @Test
        void blankFontRejected() {
            assertThrows(EditException.class, () -> FormatChange.font("  "));
            assertThrows(EditException.class, () -> FormatChange.font(null));
        }
    }

    @Nested
    class Parse {

        @Test
        void parsesEveryAttribute() {
            assertEquals(FormatChange.bold(true), FormatChange.parse(new String[] {"bold", "on"}));
            assertEquals(FormatChange.italic(false),
                    FormatChange.parse(new String[] {"italic", "off"}));
            assertEquals(FormatChange.font("Arial"),
                    FormatChange.parse(new String[] {"font", "Arial"}));
            assertEquals(FormatChange.size(14), FormatChange.parse(new String[] {"size", "14"}));
            assertEquals(FormatChange.color("red"),
                    FormatChange.parse(new String[] {"color", "red"}));
        }

        @Test
        void toStringMatchesCommandSyntax() {
            assertEquals("bold on", FormatChange.bold(true).toString());
            assertEquals("size 14", FormatChange.size(14).toString());
            assertEquals("font \"Arial\"", FormatChange.font("Arial").toString());
            assertEquals("color red", FormatChange.color("red").toString());
        }

        @Test
        void rejectsBadInput() {
            assertThrows(EditException.class, () -> FormatChange.parse("underline", "on"));
            assertThrows(EditException.class, () -> FormatChange.parse("bold", "yes"));
            assertThrows(EditException.class, () -> FormatChange.parse("size", "big"));
            assertThrows(EditException.class, () -> FormatChange.parse("size", "73"));
            assertThrows(EditException.class, () -> FormatChange.parse(new String[] {"bold"}));
        }
    }
}
