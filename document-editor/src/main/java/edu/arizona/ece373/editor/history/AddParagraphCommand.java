package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.Section;
import edu.arizona.ece373.editor.model.SectionPath;

/** Adds a paragraph with text to the end of a section (6.2.3). */
public final class AddParagraphCommand implements EditCommand {

    private final SectionPath sectionPath;
    private final String text;
    private Paragraph added;
    private int number = -1;

    /**
     * @param sectionPath the section to append to
     * @param text the paragraph text
     */
    public AddParagraphCommand(SectionPath sectionPath, String text) {
        this.sectionPath = sectionPath;
        this.text = text == null ? "" : text;
    }

    @Override
    public void validate(Document d) {
        d.section(sectionPath); // throws "No section 2.4"
    }

    @Override
    public void execute(Document d) {
        Section section = d.section(sectionPath);
        if (added == null) {
            added = new Paragraph(new FormattedText(text));
        }
        section.add(added);
        number = d.numberOf(added);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    @Override
    public void undo(Document d) {
        // Look the nodes up again: a checkpoint restore since execute may have replaced them.
        d.section(sectionPath).remove(d.paragraph(number));
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    /** @return the paragraph's number after {@link #execute}, or -1 before */
    public int addedNumber() {
        return number;
    }

    @Override
    public String describe() {
        return number < 0
                ? "add paragraph to section " + sectionPath
                : "added paragraph " + number + " to section " + sectionPath;
    }
}
