package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.model.CharFormat;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.TextRange;
import edu.arizona.ece373.editor.model.TextRun;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link FormatTextCommand}: the main case, ranges crossing existing formatting
 * boundaries, invalid ranges and values, and undo/redo round trips.
 *
 * <p>Satisfies 6.5.2, 6.5.4.
 */
class FormatTextCommandTest {

    private static final CharFormat PLAIN = CharFormat.DEFAULT;
    private static final CharFormat BOLD = PLAIN.with(FormatChange.bold(true));

    private Document doc;
    private History history;
    private final List<DocumentEvent.Kind> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        doc = new Document();
        history = new History(doc);
        history.perform(new AddParagraphCommand(null, "hello big world")); // p1
        history.perform(new AddParagraphCommand(null, "second"));          // p2
        doc.addListener(e -> events.add(e.kind()));
    }

    private FormattedText p(int n) {
        return doc.paragraph(n).text();
    }

    private static TextRange range(int p, int start, int end) {
        return new TextRange(p, start, end);
    }

    private static String[] words(String attr, String value) {
        return new String[] {attr, value};
    }

    @Test
    void mainCaseFormatsOnlyTheRange() {
        history.perform(new FormatTextCommand(range(1, 6, 9), FormatChange.bold(true)));
        assertEquals(List.of(new TextRun("hello ", PLAIN), new TextRun("big", BOLD),
                new TextRun(" world", PLAIN)), p(1).runs());
        assertEquals(List.of(new TextRun("second", PLAIN)), p(2).runs());
        assertEquals(List.of(DocumentEvent.Kind.FORMAT), events);
    }

    @Test
    void charactersNeverChange() {
        history.perform(new FormatTextCommand(range(1, 0, 15), words("color", "red")));
        history.perform(new FormatTextCommand(range(1, 2, 7), words("size", "30")));
        assertEquals("hello big world", p(1).plainText());
    }

    @Test
    void rawWordsAreParsed() {
        history.perform(new FormatTextCommand(range(1, 0, 5), words("font", "Arial")));
        assertEquals("Arial", p(1).formatAt(0).font);
        assertEquals(CharFormat.DEFAULT_FONT, p(1).formatAt(5).font);
    }

    @Test
    void rangeCrossingFormattingBoundariesKeepsOtherAttributes() {
        history.perform(new FormatTextCommand(range(1, 6, 9), FormatChange.bold(true)));
        history.perform(new FormatTextCommand(range(1, 4, 12), FormatChange.italic(true)));
        CharFormat italic = PLAIN.with(FormatChange.italic(true));
        CharFormat boldItalic = BOLD.with(FormatChange.italic(true));
        assertEquals(List.of(
                new TextRun("hell", PLAIN),
                new TextRun("o ", italic),
                new TextRun("big", boldItalic),
                new TextRun(" wo", italic),
                new TextRun("rld", PLAIN)), p(1).runs());
    }

    @Test
    void formattingWholeRunsBackToPlainMerges() {
        history.perform(new FormatTextCommand(range(1, 6, 9), FormatChange.bold(true)));
        history.perform(new FormatTextCommand(range(1, 0, 15), FormatChange.bold(false)));
        assertEquals(List.of(new TextRun("hello big world", PLAIN)), p(1).runs());
    }

    @ParameterizedTest
    @CsvSource({"1, 0, 16", "1, 5, 4", "3, 0, 1", "0, 0, 1", "1, -1, 2"})
    void invalidRangeRejectedAndNothingChanges(int para, int start, int end) {
        FormattedText before = p(1).copy();
        assertThrows(EditException.class, () -> history.perform(
                new FormatTextCommand(range(para, start, end), FormatChange.bold(true))));
        assertEquals(before, p(1));
        assertTrue(events.isEmpty());
        // Not recorded: the next undo removes the second paragraph, not a format change.
        history.undo();
        assertEquals(1, doc.paragraphs().size());
    }

    @ParameterizedTest
    @CsvSource({"size, 5", "size, 73", "size, big", "color, reddish", "bold, yes",
            "underline, on"})
    void invalidValueRejectedAndNothingChanges(String attr, String value) {
        FormattedText before = p(1).copy();
        EditException e = assertThrows(EditException.class, () -> history.perform(
                new FormatTextCommand(range(1, 0, 5), words(attr, value))));
        assertTrue(e.getMessage().contains(value) || e.getMessage().contains(attr),
                e.getMessage());
        assertEquals(before, p(1));
        assertTrue(events.isEmpty());
    }

    @Test
    void undoRestoresOldFormattingExactly() {
        history.perform(new FormatTextCommand(range(1, 6, 9), FormatChange.bold(true)));
        FormattedText before = p(1).copy();
        history.perform(new FormatTextCommand(range(1, 2, 13), FormatChange.size(40)));
        history.undo();
        assertEquals(before, p(1));
        history.undo();
        assertEquals(new FormattedText("hello big world"), p(1));
    }

    @Test
    void redoAfterUndoReappliesTheChange() {
        history.perform(new FormatTextCommand(range(1, 6, 9), FormatChange.bold(true)));
        FormattedText after = p(1).copy();
        history.undo();
        history.redo();
        assertEquals(after, p(1));
        history.undo();
        assertEquals(new FormattedText("hello big world"), p(1));
    }

    @Test
    void undoAndRedoFireFormatEvents() {
        history.perform(new FormatTextCommand(range(1, 0, 1), FormatChange.bold(true)));
        history.undo();
        history.redo();
        assertEquals(List.of(DocumentEvent.Kind.FORMAT, DocumentEvent.Kind.FORMAT,
                DocumentEvent.Kind.FORMAT), events);
    }

    @Test
    void emptyRangeIsAllowedAndChangesNothing() {
        history.perform(new FormatTextCommand(range(1, 3, 3), FormatChange.bold(true)));
        assertEquals(new FormattedText("hello big world"), p(1));
    }

    @Test
    void describeNamesRangeAndChange() {
        FormatTextCommand c = new FormatTextCommand(range(1, 6, 9), words("size", "14"));
        assertEquals("formatted p1:6-9 size 14", c.describe());
        history.perform(c);
        assertEquals("formatted p1:6-9 size 14", c.describe());
    }
}
