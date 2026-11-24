package edu.arizona.ece373.editor.view;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.DocumentListener;
import edu.arizona.ece373.editor.model.Paragraph;
import java.util.ArrayList;
import java.util.List;

/**
 * Word, character and paragraph counts that stay current without editing commands
 * computing them (6.7.1, 6.7.2, 6.7.3; design doc Figures 2b and 3, Section 5.5).
 * Pattern: Observer (concrete observer of {@link Document}).
 *
 * <p>{@link #documentChanged} only marks the counts as stale; the counting happens in
 * {@link #current()}, and only when something changed since the last count. So a Replace
 * All of 500 matches recounts once, not 500 times.
 *
 * <p><b>Counting rules (decided here):</b>
 * <ul>
 *   <li>Words are whitespace-separated tokens, so "don't" is one word and "a - b" is three.</li>
 *   <li>Characters include spaces.</li>
 *   <li>Section titles are not counted; only paragraph text is.</li>
 * </ul>
 *
 * <p>The Editor creates this and registers it with {@code document.addListener(...)}.
 */
public final class DocumentStatistics implements DocumentListener {

    private final Document document;
    private int words;
    private int chars;
    private int paragraphs;
    private boolean stale = true;

    /**
     * Creates statistics for a document.
     *
     * @param document the document to count; register this object as its listener
     */
    public DocumentStatistics(Document document) {
        this.document = document;
    }

    //Marks the counts out of date. Does not count anything
    @Override
    public void documentChanged(DocumentEvent e) {
        stale = true;
    }

    /**
     * Returns the current word, character and paragraph counts.
     *
     * @return the current counts, recounting only if the document changed
     */
    public Statistics current() {
        if (stale) {
            recount();
            stale = false;
        }
        return new Statistics(words, chars, paragraphs);
    }

    /** @return true if the next {@link #current()} will recount (package-private, for tests) */
    boolean isStale() {
        return stale;
    }

    private void recount() {
        List<Paragraph> all = new ArrayList<>();
        // Look the root up each time: a checkpoint restore replaces it.
        document.section(null).collectParagraphs(all);
        words = 0;
        chars = 0;
        for (Paragraph p : all) {
            String text = p.text().plainText();
            chars += text.length();
            words += countWords(text);
        }
        paragraphs = all.size();
    }

    private static int countWords(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }
}
