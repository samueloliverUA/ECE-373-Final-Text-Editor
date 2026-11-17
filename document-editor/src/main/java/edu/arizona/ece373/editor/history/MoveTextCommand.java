package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.TextRange;

/**
 * Moves a range of text to another position in the same paragraph (6.3.4, UC1
 * alternate flow 4c).
 *
 * <p>The source range and the destination are both positions in the paragraph as it
 * was BEFORE the move. For example, in "Hello brave world", moving p1:5-11
 * (" brave") to p1:17 gives "Hello world brave".
 *
 * <p><b>Decision:</b> the destination must be outside the source range. A destination
 * at the range's own start or end is also rejected, because the text would not change.
 */
public final class MoveTextCommand implements EditCommand {

    private final TextRange source;
    private final int destParagraph;
    private final int destOffset;
    private FormattedText moved;
    private int insertedAt = -1;

    /**
     * @param source the text to move
     * @param destParagraph the destination paragraph; must be the same as the source's
     * @param destOffset the destination, as an offset in the paragraph before the move
     */
    public MoveTextCommand(TextRange source, int destParagraph, int destOffset) {
        this.source = source;
        this.destParagraph = destParagraph;
        this.destOffset = destOffset;
    }

    @Override
    public void validate(Document d) {
        int length = d.paragraph(source.paragraph).text().length(); // throws "No paragraph 7"
        if (destParagraph != source.paragraph) {
            throw new EditException("Text can only be moved within one paragraph");
        }
        if (source.start < 0) {
            throw new EditException("Range start cannot be negative: " + source);
        }
        if (source.start > source.end) {
            throw new EditException("Range start is after its end: " + source);
        }
        if (source.end > length) {
            throw new EditException("p" + source.paragraph + " has " + length + " characters");
        }
        if (source.start == source.end) {
            throw new EditException("Nothing to move: " + source + " is empty");
        }
        if (destOffset < 0 || destOffset > length) {
            throw new EditException("p" + source.paragraph + " has " + length + " characters");
        }
        if (destOffset >= source.start && destOffset <= source.end) {
            throw new EditException("Destination p" + destParagraph + ":" + destOffset
                    + " must be outside " + source);
        }
    }

    @Override
    public void execute(Document d) {
        FormattedText text = d.paragraph(source.paragraph).text();
        moved = text.cut(source.start, source.end);
        // The destination was measured before the cut. If it was after the source,
        // everything after the source has shifted left by the moved length.
        if (destOffset > source.end) {
            insertedAt = destOffset - moved.length();
        } else {
            insertedAt = destOffset;
        }
        text.insert(insertedAt, moved);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    @Override
    public void undo(Document d) {
        // Exact reverse: take the text out of where it went, put it back where it was.
        FormattedText text = d.paragraph(source.paragraph).text();
        FormattedText piece = text.cut(insertedAt, insertedAt + moved.length());
        text.insert(source.start, piece);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
    }

    @Override
    public String describe() {
        String where = source + " to p" + destParagraph + ":" + destOffset;
        if (moved == null) {
            return "move " + where;
        } else {
            return "moved " + moved.length() + " characters from " + where;
        }
    }
}
