package edu.arizona.ece373.editor.view;

import edu.arizona.ece373.editor.model.CharFormat;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.TextRun;
import java.util.ArrayList;
import java.util.List;

/**
 * Marked-up view: each word is wrapped in markers that show its formatting. Only attributes
 * that differ from {@link CharFormat#DEFAULT} get a marker.
 *
 * <p>Marker syntax (keep the demo script identical):
 * <pre>
 *   bold          *word*
 *   italic        _word_
 *   size 14       [size=14: word]
 *   font Arial    [font=Arial: word]
 *   color red     [color=red: word]
 * </pre>
 * Markers nest in that order, innermost first, so a bold italic red word is
 * {@code [color=red: _*word*_]}. Each word is marked on its own, so a bold phrase shows as
 * {@code *two* *words*}. A word whose formatting changes part-way is marked piece by piece:
 * {@code *bo*ld}.
 *
 * <p>Markers count toward the line width: each marked word is one unbreakable word for the
 * line breaker.
 *
 * <p>Satisfies 6.6.2.
 */
public final class MarkupRenderer extends AbstractRenderer {

    /** Creates a renderer that shows formatting markers. */
    public MarkupRenderer() {
    }

    @Override
    protected List<String> tokens(FormattedText t) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        StringBuilder piece = new StringBuilder();
        CharFormat pieceFormat = null;

        for (TextRun run : t.runs()) {
            for (int i = 0; i < run.text.length(); i++) {
                char c = run.text.charAt(i);
                if (Character.isWhitespace(c)) {
                    flushPiece(word, piece, pieceFormat);
                    flushWord(out, word);
                } else {
                    if (piece.length() > 0 && !run.format.equals(pieceFormat)) {
                        flushPiece(word, piece, pieceFormat);
                    }
                    pieceFormat = run.format;
                    piece.append(c);
                }
            }
        }
        flushPiece(word, piece, pieceFormat);
        flushWord(out, word);
        return out;
    }

    /**
     * @param text characters that all have formatting {@code f}
     * @param f their formatting
     * @return the text wrapped in the markers for {@code f}
     */
    static String mark(String text, CharFormat f) {
        String s = text;
        if (f.bold) {
            s = "*" + s + "*";
        }
        if (f.italic) {
            s = "_" + s + "_";
        }
        if (f.size != CharFormat.DEFAULT_SIZE) {
            s = "[size=" + f.size + ": " + s + "]";
        }
        if (!f.font.equals(CharFormat.DEFAULT_FONT)) {
            s = "[font=" + f.font + ": " + s + "]";
        }
        if (!f.color.equals(CharFormat.DEFAULT_COLOR)) {
            s = "[color=" + f.color + ": " + s + "]";
        }
        return s;
    }

    private static void flushPiece(StringBuilder word, StringBuilder piece, CharFormat f) {
        if (piece.length() > 0) {
            word.append(mark(piece.toString(), f));
            piece.setLength(0);
        }
    }

    private static void flushWord(List<String> out, StringBuilder word) {
        if (word.length() > 0) {
            out.add(word.toString());
            word.setLength(0);
        }
    }
}
