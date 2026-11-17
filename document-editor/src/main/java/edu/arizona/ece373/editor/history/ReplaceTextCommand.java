package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.TextRange;

/**
 * Replaces a range of text with new text (6.3.3). Also used for replace-first and
 * Replace All (6.8.2, 6.8.3; design doc Figures 4 and 5).
 *
 * <p><b>Formatting decision:</b> the new text takes the formatting of the first
 * replaced character, so replacing a bold word gives a bold word. Undo puts the old
 * text back with its own formatting.
 *
 * <p><b>Other decisions:</b> the range must not be empty (to add text with no
 * removal, use {@link InsertTextCommand}). The new text may be empty, which deletes
 * the range; Replace All with "" relies on this.
 *
 * <p><b>Replace All:</b> put one ReplaceTextCommand per match in a
 * {@link CompositeCommand}, adding the matches from LAST to FIRST. Each replacement
 * then only moves text after itself, so every range found by the search stays valid,
 * both for validate (which checks all parts against the original document) and for
 * execute.
 */
public final class ReplaceTextCommand implements EditCommand {

    private final TextRange range;
    private final String newText;
    private FormattedText removed;

    /**
     * @param range the text to replace (end exclusive)
     * @param newText the replacement; null is treated as empty
     */
    public ReplaceTextCommand(TextRange range, String newText) {
        this.range = range;
        if (newText == null) {
            this.newText = "";
        } else {
            this.newText = newText;
        }
    }

    @Override
    public void validate(Document d) {
        int length = d.paragraph(range.paragraph).text().length(); // throws "No paragraph 7"
        if (range.start < 0) {
            throw new EditException("Range start cannot be negative: " + range);
        }
        if (range.start > range.end) {
            throw new EditException("Range start is after its end: " + range);
        }
        if (range.end > length) {
            throw new EditException("p" + range.paragraph + " has " + length + " characters");
        }
        if (range.start == range.end) {
            throw new EditException("Nothing to replace: " + range + " is empty");
        }
    }

    @Override
    public void execute(Document d) {
        FormattedText text = d.paragraph(range.paragraph).text();
        removed = text.cut(range.start, range.end);
        // Formatting decision (see class comment): take the first replaced character's format.
        if (removed.isEmpty()) {
            text.insert(range.start, newText);
        } else {
            text.insert(range.start, new FormattedText(newText, removed.formatAt(0)));
        }
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    @Override
    public void undo(Document d) {
        // Look the paragraph up again: a checkpoint restore since execute may have replaced it.
        FormattedText text = d.paragraph(range.paragraph).text();
        text.cut(range.start, range.start + newText.length());
        text.insert(range.start, removed);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    /** @return the text that was replaced, or null before {@link #execute} */
    public FormattedText removedText() {
        return removed;
    }

    @Override
    public String describe() {
        if (removed == null) {
            return "replace " + range + " with \"" + newText + "\"";
        } else {
            return "replaced \"" + removed.plainText() + "\" with \"" + newText + "\" at " + range;
        }
    }
}
