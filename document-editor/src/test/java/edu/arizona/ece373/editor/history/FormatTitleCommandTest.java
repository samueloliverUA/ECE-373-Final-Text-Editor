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
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRun;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link FormatTitleCommand}: the main case, titles that already have mixed
 * formatting, missing sections and invalid values, and undo/redo round trips.
 *
 * <p>Satisfies 6.5.5.
 */
class FormatTitleCommandTest {

    private static final CharFormat PLAIN = CharFormat.DEFAULT;

    private Document doc;
    private History history;
    private final List<DocumentEvent.Kind> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        doc = new Document();
        history = new History(doc);
        history.perform(new AddSectionCommand(null, "Introduction"));              // 1
        history.perform(new AddSectionCommand(null, "Methods"));                   // 2
        history.perform(new AddSectionCommand(SectionPath.parse("2"), "Setup"));   // 2.1
        history.perform(new AddParagraphCommand(SectionPath.parse("1"), "body"));  // p1
        doc.addListener(e -> events.add(e.kind()));
    }

    private FormattedText title(String path) {
        return doc.section(SectionPath.parse(path)).title();
    }

    private static SectionPath path(String p) {
        return SectionPath.parse(p);
    }

    @Test
    void mainCaseFormatsWholeTitleOnly() {
        history.perform(new FormatTitleCommand(path("1"), FormatChange.size(18)));
        assertEquals(List.of(new TextRun("Introduction", PLAIN.with(FormatChange.size(18)))),
                title("1").runs());
        assertEquals(new FormattedText("Methods"), title("2"));
        assertEquals(new FormattedText("body"), doc.paragraph(1).text());
        assertEquals(List.of(DocumentEvent.Kind.FORMAT), events);
    }

    @Test
    void charactersNeverChange() {
        history.perform(new FormatTitleCommand(path("2.1"), new String[] {"color", "blue"}));
        assertEquals("Setup", title("2.1").plainText());
    }

    @Test
    void nestedSectionByRawWords() {
        history.perform(new FormatTitleCommand(path("2.1"), new String[] {"bold", "on"}));
        assertTrue(title("2.1").formatAt(0).bold);
        assertEquals(new FormattedText("Methods"), title("2"));
    }

    @Test
    void titleWithMixedFormattingKeepsOtherAttributes() {
        title("2").applyFormat(0, 3, FormatChange.italic(true)); // "Met" italic, "hods" plain
        history.perform(new FormatTitleCommand(path("2"), FormatChange.bold(true)));
        CharFormat bold = PLAIN.with(FormatChange.bold(true));
        assertEquals(List.of(new TextRun("Met", bold.with(FormatChange.italic(true))),
                new TextRun("hods", bold)), title("2").runs());
    }

    @Test
    void mixedTitleMergesWhenAttributeMadeUniform() {
        title("2").applyFormat(0, 3, FormatChange.italic(true));
        history.perform(new FormatTitleCommand(path("2"), FormatChange.italic(true)));
        assertEquals(1, title("2").runs().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3", "2.2", "1.1", "2.1.1"})
    void missingSectionRejectedAndNothingChanges(String missing) {
        EditException e = assertThrows(EditException.class, () -> history.perform(
                new FormatTitleCommand(path(missing), FormatChange.bold(true))));
        assertTrue(e.getMessage().contains("No section"), e.getMessage());
        assertTrue(events.isEmpty());
    }

    @Test
    void nullSectionRejected() {
        assertThrows(EditException.class, () -> history.perform(
                new FormatTitleCommand(null, FormatChange.bold(true))));
        assertEquals(new FormattedText(""), doc.root().title());
    }

    @ParameterizedTest
    @CsvSource({"size, 5", "size, 73", "color, #12345", "italic, maybe", "weight, 3"})
    void invalidValueRejectedAndNothingChanges(String attr, String value) {
        FormattedText before = title("1").copy();
        assertThrows(EditException.class, () -> history.perform(
                new FormatTitleCommand(path("1"), new String[] {attr, value})));
        assertEquals(before, title("1"));
        assertTrue(events.isEmpty());
        // Not recorded: the next undo removes the paragraph added in setUp.
        history.undo();
        assertEquals(0, doc.paragraphs().size());
    }

    @Test
    void undoRestoresOldFormattingExactly() {
        title("2").applyFormat(2, 5, FormatChange.color("red"));
        FormattedText before = title("2").copy();
        history.perform(new FormatTitleCommand(path("2"), FormatChange.font("Arial")));
        history.undo();
        assertEquals(before, title("2"));
    }

    @Test
    void redoAfterUndoReappliesTheChange() {
        history.perform(new FormatTitleCommand(path("1"), FormatChange.size(24)));
        FormattedText after = title("1").copy();
        history.undo();
        assertEquals(new FormattedText("Introduction"), title("1"));
        history.redo();
        assertEquals(after, title("1"));
    }

    @Test
    void describeNamesSectionAndChange() {
        assertEquals("formatted title of section 2.1 bold on",
                new FormatTitleCommand(path("2.1"), new String[] {"bold", "on"}).describe());
    }
}
