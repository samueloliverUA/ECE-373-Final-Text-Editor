package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.Paragraph;

/** Inserts text at a position in a paragraph (6.3.1, design doc Figure 3). */
public final class InsertTextCommand implements EditCommand {

    private final int paragraph;
    private final int offset;
    private final String text;

    /**
     * Creates a command that inserts text into a paragraph.
     *
     * @param paragraph the paragraph number, counting from 1
     * @param offset where to insert; 0 is the start, the length is the end
     * @param text the text to insert
     */
    public InsertTextCommand(int paragraph, int offset, String text) {
        this.paragraph = paragraph;
        this.offset = offset;
        if (text == null) {
            this.text = "";
        } else {
            this.text = text;
        }
    }

    @Override
    public void validate(Document d) {
        Paragraph p = d.paragraph(paragraph); // throws "No paragraph 7"
        int length = p.text().length();
        if (offset < 0 || offset > length) {
            throw new EditException("p" + paragraph + " has " + length + " characters");
        }
        if (text.isEmpty()) {
            throw new EditException("Nothing to insert");
        }
    }

    @Override
    public void execute(Document d) {
        // The String version of insert gives the new text its neighbour's formatting,
        // so typing inside a bold word stays bold.
        d.paragraph(paragraph).text().insert(offset, text);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    @Override
    public void undo(Document d) {
        // Look the paragraph up again: a checkpoint restore since execute may have replaced it.
        d.paragraph(paragraph).text().cut(offset, offset + text.length());
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    @Override
    public String describe() {
        return "insert " + text.length() + " characters at p" + paragraph + ":" + offset;
    }
}
