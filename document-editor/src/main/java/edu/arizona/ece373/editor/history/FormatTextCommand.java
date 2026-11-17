package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.TextRange;
import edu.arizona.ece373.editor.model.TextRun;
import java.util.List;

/**
 * Applies one formatting change to a range of characters, e.g.
 * {@code format p2:4-25 bold on}. The characters themselves never change (6.5.4); only
 * their formatting does.
 *
 * <p>Execute keeps the runs {@code applyFormat} returns, and undo puts them back, so an undo
 * restores the old formatting exactly, including run boundaries.
 *
 * <p>The change may be given already built, or as the raw {@code {attribute, value}} words
 * the parser read; raw words are checked in {@link #validate}, so a bad value such as
 * {@code size 99} becomes an ordinary error result rather than an exception.
 *
 * <p>Satisfies 6.5.2, 6.5.3, 6.5.4. Design doc UC3; Section 5.6.
 */
public final class FormatTextCommand implements EditCommand {

    private final TextRange range;
    private final String[] raw;
    private FormatChange change;
    private List<TextRun> oldRuns;

    /**
     * @param range the characters to format
     * @param change the attribute and its new value
     */
    public FormatTextCommand(TextRange range, FormatChange change) {
        this.range = range;
        this.change = change;
        this.raw = null;
    }

    /**
     * @param range the characters to format
     * @param attrAndValue the attribute and value as typed, e.g. {@code {"size", "14"}};
     *     checked by {@link #validate}
     */
    public FormatTextCommand(TextRange range, String[] attrAndValue) {
        this.range = range;
        this.raw = attrAndValue.clone();
    }

    @Override
    public void validate(Document d) {
        if (range == null) {
            throw new EditException("format needs a range like p2:5-12");
        }
        if (change == null) {
            change = FormatChange.parse(raw); // throws on a bad attribute or value
        }
        range.validate(d); // throws "No paragraph 3" or "p2 has 10 characters"
    }

    @Override
    public void execute(Document d) {
        oldRuns = d.paragraph(range.paragraph).text()
                .applyFormat(range.start, range.end, change);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.FORMAT));
    }

    @Override
    public void undo(Document d) {
        d.paragraph(range.paragraph).text().restoreRuns(oldRuns);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.FORMAT));
    }

    @Override
    public String describe() {
        return "formatted " + range + " " + changeText();
    }

    private String changeText() {
        return change != null ? change.toString() : String.join(" ", raw);
    }
}
