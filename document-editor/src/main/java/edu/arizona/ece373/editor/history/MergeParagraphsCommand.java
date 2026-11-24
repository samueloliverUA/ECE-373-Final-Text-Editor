package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.Section;

/**
 * Appends the following paragraph to paragraph n and removes it (6.3.6, UC1 alternate
 * flow 4b).
 *
 * <p><b>Decision:</b> both paragraphs must be in the same section. Merging the last
 * paragraph of a section with the first paragraph of the next section (or of a
 * subsection) is rejected, because it is not clear which section the result belongs to.
 */
public final class MergeParagraphsCommand implements EditCommand {

    private final int number;
    private Paragraph second;
    private int boundary = -1;
    private int secondIndex = -1;

    /**
     * Creates a command that merges a paragraph with the next one.
     *
     * @param number the paragraph to merge into; paragraph number + 1 is appended to it
     */
    public MergeParagraphsCommand(int number) {
        this.number = number;
    }

    @Override
    public void validate(Document d) {
        Paragraph first = d.paragraph(number); // throws "No paragraph 7"
        Paragraph next;
        try {
            next = d.paragraph(number + 1);
        } catch (EditException e) {
            throw new EditException("p" + number + " is the last paragraph; nothing to merge");
        }
        Section root = d.section(null);
        if (sectionOf(root, first) != sectionOf(root, next)) {
            throw new EditException("p" + number + " and p" + (number + 1)
                    + " are in different sections");
        }
    }

    @Override
    public void execute(Document d) {
        Paragraph first = d.paragraph(number);
        second = d.paragraph(number + 1);
        Section parent = sectionOf(d.section(null), first);

        boundary = first.text().length();
        secondIndex = parent.indexOf(second);
        first.text().append(second.text());
        parent.remove(second);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    @Override
    public void undo(Document d) {
        Paragraph first = d.paragraph(number);
        first.text().splitAt(boundary); // drops the appended text from the first paragraph
        Section parent = sectionOf(d.section(null), first);
        parent.add(secondIndex, second); // the original second paragraph, text unchanged
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    /**
     * Returns the offset where the second paragraph's text starts.
     *
     * @return the offset where the second paragraph's text starts, or -1 before execute
     */
    public int boundary() {
        return boundary;
    }

    @Override
    public String describe() {
        if (boundary < 0) {
            return "merge p" + number + " with p" + (number + 1);
        } else {
            return "merged p" + (number + 1) + " into p" + number;
        }
    }

    //Finds the section that directly holds paragraph p, searching from s down
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
