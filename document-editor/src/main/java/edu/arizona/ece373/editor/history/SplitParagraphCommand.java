package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.Section;

/**
 * Splits a paragraph at an offset into two paragraphs in the same section (6.3.5,
 * UC1 alternate flow 4a). The text after the offset, with its formatting, becomes a
 * new paragraph right after the original.
 *
 * <p>Offset 0 leaves the original paragraph empty, and offset = length gives an empty
 * new paragraph. Both are allowed, because that is how a user starts a new paragraph
 * before or after one.
 */
public final class SplitParagraphCommand implements EditCommand {

    private final int number;
    private final int offset;
    private boolean done;

    /**
     * Creates a command that splits a paragraph in two.
     *
     * @param number the paragraph to split, counting from 1
     * @param offset where to split, 0 to the paragraph's length
     */
    public SplitParagraphCommand(int number, int offset) {
        this.number = number;
        this.offset = offset;
    }

    @Override
    public void validate(Document d) {
        int length = d.paragraph(number).text().length(); // throws "No paragraph 7"
        if (offset < 0 || offset > length) {
            throw new EditException("p" + number + " has " + length + " characters");
        }
    }

    @Override
    public void execute(Document d) {
        Paragraph first = d.paragraph(number);
        Section parent = sectionOf(d.section(null), first);
        FormattedText tail = first.text().splitAt(offset);
        parent.add(parent.indexOf(first) + 1, new Paragraph(tail));
        done = true;
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    @Override
    public void undo(Document d) {
        // Look the paragraphs up again: a checkpoint restore since execute may have replaced them.
        Paragraph first = d.paragraph(number);
        Paragraph second = d.paragraph(number + 1);
        first.text().append(second.text());
        sectionOf(d.section(null), second).remove(second);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    @Override
    public String describe() {
        if (done) {
            return "split p" + number + " at " + offset + " into p" + number + " and p" + (number + 1);
        } else {
            return "split p" + number + " at " + offset;
        }
    }

    /** Finds the section that directly holds paragraph p, searching from s down. */
    private static Section sectionOf(Section s, Paragraph p) {
        if (s.indexOf(p) >= 0) {
            return s;
        }
        for (Section sub : s.subsections()) {
            Section found = sectionOf(sub, p);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
