package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link FormatPool} (stretch goal, 6.5.6).
 *
 * <p>Covers equal formats giving back the same instance, size() counting distinct
 * formats, and CharFormat routing its formats through the shared pool.
 *
 * <p>Most tests use their own {@code new FormatPool()} so they start empty and do not
 * depend on each other. The shared pool is used by the whole program, so its exact
 * size depends on what ran before; those tests only check that it does not grow.
 */
class FormatPoolTest {

    //equal formats return the same instance

    @Test
    void firstInternReturnsTheSameObject() {
        FormatPool pool = new FormatPool();
        CharFormat bold = new CharFormat(true, false, "Arial", 12, "black");

        assertSame(bold, pool.intern(bold));
    }

    @Test
    void equalFormatGivesBackTheStoredOne() {
        FormatPool pool = new FormatPool();
        CharFormat first = new CharFormat(true, false, "Arial", 12, "black");
        CharFormat second = new CharFormat(true, false, "Arial", 12, "black");
        assertNotSame(first, second); // two separate objects to start with

        pool.intern(first);
        CharFormat result = pool.intern(second);

        assertSame(first, result);
    }

    @Test
    void formatsThatDifferInAnyAttributeAreKeptApart() {
        FormatPool pool = new FormatPool();
        CharFormat base = pool.intern(new CharFormat(false, false, "Arial", 12, "black"));

        CharFormat bold = pool.intern(new CharFormat(true, false, "Arial", 12, "black"));
        CharFormat italic = pool.intern(new CharFormat(false, true, "Arial", 12, "black"));
        CharFormat font = pool.intern(new CharFormat(false, false, "Courier", 12, "black"));
        CharFormat size = pool.intern(new CharFormat(false, false, "Arial", 20, "black"));
        CharFormat color = pool.intern(new CharFormat(false, false, "Arial", 12, "red"));

        assertNotSame(base, bold);
        assertNotSame(base, italic);
        assertNotSame(base, font);
        assertNotSame(base, size);
        assertNotSame(base, color);
        assertEquals(6, pool.size());
    }

    @Test
    void internNeverChangesTheFormat() {
        FormatPool pool = new FormatPool();
        CharFormat f = new CharFormat(true, true, "Arial", 14, "#ff0000");

        CharFormat result = pool.intern(f);

        assertTrue(result.bold);
        assertTrue(result.italic);
        assertEquals("Arial", result.font);
        assertEquals(14, result.size);
        assertEquals("#ff0000", result.color);
    }

    @Test
    void nullIsRejected() {
        FormatPool pool = new FormatPool();

        assertThrows(NullPointerException.class, () -> pool.intern(null));
        assertEquals(0, pool.size());
    }

    //size() counts distinct formats

    @Test
    void newPoolIsEmpty() {
        assertEquals(0, new FormatPool().size());
    }

    @Test
    void sizeCountsDistinctFormatsOnly() {
        FormatPool pool = new FormatPool();

        for (int i = 0; i < 100; i++) {
            pool.intern(new CharFormat(true, false, "Arial", 12, "black"));
            pool.intern(new CharFormat(false, false, "Arial", 12, "black"));
        }

        assertEquals(2, pool.size());
    }

    @Test
    void sizeGrowsByOneForEachNewFormat() {
        FormatPool pool = new FormatPool();

        pool.intern(new CharFormat(false, false, "Arial", 12, "black"));
        assertEquals(1, pool.size());

        pool.intern(new CharFormat(false, false, "Arial", 13, "black"));
        assertEquals(2, pool.size());

        pool.intern(new CharFormat(false, false, "Arial", 12, "black")); // already there
        assertEquals(2, pool.size());
    }

    @Test
    void separatePoolsDoNotShare() {
        FormatPool a = new FormatPool();
        FormatPool b = new FormatPool();
        CharFormat first = new CharFormat(true, false, "Arial", 12, "black");
        CharFormat second = new CharFormat(true, false, "Arial", 12, "black");

        assertSame(first, a.intern(first));
        assertSame(second, b.intern(second));
        assertEquals(1, a.size());
        assertEquals(1, b.size());
    }

    //the shared pool, used by CharFormat

    @Test
    void sharedReturnsTheSamePoolEveryTime() {
        assertSame(FormatPool.shared(), FormatPool.shared());
    }

    @Test
    void defaultIsInTheSharedPool() {
        CharFormat plain = new CharFormat(false, false, CharFormat.DEFAULT_FONT,
                CharFormat.DEFAULT_SIZE, CharFormat.DEFAULT_COLOR);

        assertSame(CharFormat.DEFAULT, FormatPool.shared().intern(plain));
    }

    @Test
    void withGivesBackOneSharedObjectForEqualFormats() {
        CharFormat a = CharFormat.DEFAULT.with(FormatChange.bold(true));
        CharFormat b = CharFormat.DEFAULT.with(FormatChange.bold(true));

        assertSame(a, b);
    }

    @Test
    void differentRoutesToTheSameFormatShareOneObject() {
        // bold then italic, and italic then bold, end up as the same format
        CharFormat boldItalic = CharFormat.DEFAULT
                .with(FormatChange.bold(true)).with(FormatChange.italic(true));
        CharFormat italicBold = CharFormat.DEFAULT
                .with(FormatChange.italic(true)).with(FormatChange.bold(true));

        assertSame(boldItalic, italicBold);
    }

    @Test
    void undoingAChangeGivesBackDefaultItself() {
        CharFormat back = CharFormat.DEFAULT
                .with(FormatChange.bold(true))
                .with(FormatChange.bold(false));

        assertSame(CharFormat.DEFAULT, back);
    }

    @Test
    void formattingTextRepeatedlyDoesNotGrowTheSharedPool() {
        FormattedText warmUp = new FormattedText("x");
        warmUp.applyFormat(0, 1, FormatChange.bold(true)); // make sure bold is stored
        int before = FormatPool.shared().size();

        FormattedText t = new FormattedText("Hello world");
        for (int i = 0; i < t.length(); i++) {
            t.applyFormat(i, i + 1, FormatChange.bold(true));
        }

        // Eleven bolded characters, but no new formats: they all share one bold object.
        assertEquals(before, FormatPool.shared().size());
        assertSame(t.formatAt(0), warmUp.formatAt(0));
    }
}
