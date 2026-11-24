package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.TextRange;

/**
 * Deletes a range of text from one paragraph, remembering it (with formatting)
 * so undo can put it back exactly (6.3.2, design doc 5.2).
 */
public final class DeleteTextCommand implements EditCommand {

    private final TextRange range;
    private FormattedText removed;

    /**
     * Creates a command that deletes a range of text.
     *
     * @param range the paragraph and characters to delete (end exclusive)
     */
    public DeleteTextCommand(TextRange range) {
        this.range = range;
    }

    @Override
    public void validate(Document d) {
        Paragraph p = d.paragraph(range.paragraph); // throws "No paragraph 7"
        int length = p.text().length();
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
            throw new EditException("Nothing to delete: " + range + " is empty");
        }
    }

    @Override
    public void execute(Document d) {
        // cut() returns the removed runs with their formatting; keep them for undo.
        removed = d.paragraph(range.paragraph).text().cut(range.start, range.end);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    @Override
    public void undo(Document d) {
        // Look the paragraph up again: a checkpoint restore since execute may have replaced it.
        d.paragraph(range.paragraph).text().insert(range.start, removed);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    /**
     * Returns the text this command removed.
     *
     * @return the text removed by {@link #execute}, or null before
     */
    public FormattedText removedText() {
        return removed;
    }

    @Override
    public String describe() {
        if (removed == null) {
            return "delete " + range;
        } else {
            return "deleted " + removed.length() + " characters from " + range;
        }
    }
}
