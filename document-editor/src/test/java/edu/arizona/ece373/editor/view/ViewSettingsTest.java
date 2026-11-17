package edu.arizona.ece373.editor.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link ViewSettings}: defaults, switching view and breaker at runtime changes the
 * output, and width limits 10-200 with the old width kept on rejection.
 *
 * <p>Satisfies 6.6.3, 6.6.4, 6.6.7.
 */
class ViewSettingsTest {

    private final ViewSettings settings = new ViewSettings();

    private static Document doc(String text) {
        Document d = new Document();
        d.root().add(new Paragraph(new FormattedText(text)));
        return d;
    }

    @Test
    void defaults() {
        assertInstanceOf(PlainTextRenderer.class, settings.renderer());
        assertInstanceOf(GreedyLineBreaker.class, settings.breaker());
        assertEquals(80, settings.width());
    }

    @Test
    void switchingViewChangesOutput() {
        Document d = doc("one two");
        d.paragraph(1).text().applyFormat(0, 3, FormatChange.bold(true));
        assertEquals("one two", settings.render(d));
        settings.setView("markup");
        assertInstanceOf(MarkupRenderer.class, settings.renderer());
        assertEquals("*one* two", settings.render(d));
        settings.setView("plain");
        assertEquals("one two", settings.render(d));
    }

    @Test
    void switchingBreakerChangesOutput() {
        Document d = doc("aaaaa bbbb cc dddddddddd");
        settings.setWidth(10);
        String greedy = settings.render(d);
        settings.setBreaker("balanced");
        assertInstanceOf(BalancedLineBreaker.class, settings.breaker());
        String balanced = settings.render(d);
        assertEquals("aaaaa bbbb\ncc\ndddddddddd", greedy);
        assertEquals("aaaaa\nbbbb cc\ndddddddddd", balanced);
        assertNotEquals(greedy, balanced);
        settings.setBreaker("greedy");
        assertEquals(greedy, settings.render(d));
    }

    @Test
    void switchingWidthChangesOutput() {
        Document d = doc("aaa bb cc ddddd");
        settings.setWidth(10);
        assertEquals("aaa bb cc\nddddd", settings.render(d));
        settings.setWidth(20);
        assertEquals("aaa bb cc ddddd", settings.render(d));
    }

    @Test
    void settersTakeObjectsToo() {
        settings.setRenderer(new MarkupRenderer());
        settings.setBreaker(new BalancedLineBreaker());
        assertInstanceOf(MarkupRenderer.class, settings.renderer());
        assertInstanceOf(BalancedLineBreaker.class, settings.breaker());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 11, 199, 200})
    void widthInRangeAccepted(int w) {
        settings.setWidth(w);
        assertEquals(w, settings.width());
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 201, 0, -5})
    void widthOutOfRangeRejectedAndOldWidthKept(int w) {
        settings.setWidth(40);
        EditException e = assertThrows(EditException.class, () -> settings.setWidth(w));
        assertEquals("width must be 10-200, got " + w + "; width is still 40", e.getMessage());
        assertEquals(40, settings.width());
    }

    @Test
    void unknownViewOrBreakerRejectedAndKept() {
        settings.setView("markup");
        assertThrows(EditException.class, () -> settings.setView("fancy"));
        assertInstanceOf(MarkupRenderer.class, settings.renderer());
        assertThrows(EditException.class, () -> settings.setBreaker("optimal"));
        assertInstanceOf(GreedyLineBreaker.class, settings.breaker());
    }
}
