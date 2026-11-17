package edu.arizona.ece373.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A string where different characters carry different formatting, stored as a list of
 * {@link TextRun}s.
 *
 * <p>Invariants, restored after every change: no run is empty, and no two neighbouring runs
 * have equal formatting (they are merged). An empty text therefore has no runs.
 *
 * <p>Offsets are zero-based; ranges are {@code [start, end)}. A bad offset throws
 * {@link EditException} and leaves the text unchanged.
 *
 * <p>Plain text inserted with {@link #insert(int, String)} takes the formatting of the
 * character just before the insertion point; at offset 0 it takes the formatting of the
 * first character, and in an empty text it gets {@link CharFormat#DEFAULT}.
 *
 * <p>Satisfies 6.3.x, 6.5.2, 6.5.3. Design doc Fig 2b, Section 5.6.
 */
public final class FormattedText {

    private final List<TextRun> runs = new ArrayList<>();

    /**
     * @param plain the initial text, with default formatting (null is treated as empty)
     */
    public FormattedText(String plain) {
        this(plain, CharFormat.DEFAULT);
    }

    /**
     * @param plain the initial text (null is treated as empty)
     * @param format the formatting of every character
     */
    public FormattedText(String plain, CharFormat format) {
        if (plain != null && !plain.isEmpty()) {
            runs.add(new TextRun(plain, format));
        }
    }

    /**
     * @param runs the runs, in order; empty runs are dropped and equal neighbours merged
     */
    public FormattedText(List<TextRun> runs) {
        this.runs.addAll(runs);
        normalize();
    }

    // ---------------------------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------------------------

    /** @return the number of characters */
    public int length() {
        int n = 0;
        for (TextRun r : runs) {
            n += r.length();
        }
        return n;
    }

    /** @return true if there are no characters */
    public boolean isEmpty() {
        return runs.isEmpty();
    }

    /** @return the characters without formatting */
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (TextRun r : runs) {
            sb.append(r.text);
        }
        return sb.toString();
    }

    /** @return the runs in order, read-only */
    public List<TextRun> runs() {
        return Collections.unmodifiableList(new ArrayList<>(runs));
    }

    /**
     * @param offset a character offset, 0 to {@code length() - 1}
     * @return the formatting of that character
     * @throws EditException if there is no character at {@code offset}
     */
    public CharFormat formatAt(int offset) {
        if (offset < 0 || offset >= length()) {
            throw new EditException("no character at offset " + offset + "; text has "
                    + length() + " characters");
        }
        int pos = 0;
        for (TextRun r : runs) {
            if (offset < pos + r.length()) {
                return r.format;
            }
            pos += r.length();
        }
        throw new IllegalStateException("unreachable");
    }

    /** @return an independent copy */
    public FormattedText copy() {
        return new FormattedText(runs);
    }

    // ---------------------------------------------------------------------------------------
    // Changes
    // ---------------------------------------------------------------------------------------

    /**
     * Inserts plain text, which takes the formatting of its neighbour (see class comment).
     *
     * @param offset where to insert, 0 to {@code length()}
     * @param text the characters to insert
     * @throws EditException if {@code offset} is out of range
     */
    public void insert(int offset, String text) {
        checkOffset(offset);
        if (text == null || text.isEmpty()) {
            return;
        }
        CharFormat format;
        if (offset > 0) {
            format = formatAt(offset - 1);
        } else if (!runs.isEmpty()) {
            format = runs.get(0).format;
        } else {
            format = CharFormat.DEFAULT;
        }
        insert(offset, new FormattedText(text, format));
    }

    /**
     * Inserts formatted text, keeping its formatting (used to undo a cut).
     *
     * @param offset where to insert, 0 to {@code length()}
     * @param text the text to insert; not changed
     * @throws EditException if {@code offset} is out of range
     */
    public void insert(int offset, FormattedText text) {
        checkOffset(offset);
        List<TextRun> incoming = new ArrayList<>(text.runs);
        runs.addAll(splitAt0(offset), incoming);
        normalize();
    }

    /**
     * Removes a range and returns it with its formatting.
     *
     * @param start first offset removed
     * @param end offset after the last one removed
     * @return the removed text
     * @throws EditException if the range is out of bounds or {@code start > end}
     */
    public FormattedText cut(int start, int end) {
        checkRange(start, end);
        int a = splitAt0(start);
        int b = splitAt0(end);
        List<TextRun> removed = runs.subList(a, b);
        FormattedText out = new FormattedText(new ArrayList<>(removed));
        removed.clear();
        normalize();
        return out;
    }

    /**
     * Cuts everything from {@code offset} to the end and returns it (used to split a
     * paragraph in two).
     *
     * @param offset where to split, 0 to {@code length()}
     * @return the tail; this text keeps the head
     * @throws EditException if {@code offset} is out of range
     */
    public FormattedText splitAt(int offset) {
        checkOffset(offset);
        return cut(offset, length());
    }

    /**
     * Adds text to the end, keeping its formatting (used to merge paragraphs).
     *
     * @param other the text to append; not changed
     */
    public void append(FormattedText other) {
        runs.addAll(new ArrayList<>(other.runs));
        normalize();
    }

    /**
     * Changes one formatting attribute on a range. The characters themselves never change
     * (6.5.4).
     *
     * @param start first offset formatted
     * @param end offset after the last one formatted
     * @param change the attribute and its new value
     * @return the runs as they were before, for {@link #restoreRuns(List)} on undo
     * @throws EditException if the range is out of bounds or {@code start > end}
     */
    public List<TextRun> applyFormat(int start, int end, FormatChange change) {
        checkRange(start, end);
        List<TextRun> before = runs();
        int a = splitAt0(start);
        int b = splitAt0(end);
        for (int i = a; i < b; i++) {
            TextRun r = runs.get(i);
            runs.set(i, r.withFormat(r.format.with(change)));
        }
        normalize();
        return before;
    }

    /**
     * Replaces every run, e.g. with the list {@link #applyFormat} returned (undo).
     *
     * @param saved the runs to put back
     */
    public void restoreRuns(List<TextRun> saved) {
        runs.clear();
        runs.addAll(saved);
        normalize();
    }

    // ---------------------------------------------------------------------------------------
    // Object
    // ---------------------------------------------------------------------------------------

    /** Equal when the characters and their formatting are equal. */
    @Override
    public boolean equals(Object o) {
        return o instanceof FormattedText t && t.runs.equals(runs);
    }

    @Override
    public int hashCode() {
        return runs.hashCode();
    }

    /** @return the characters without formatting */
    @Override
    public String toString() {
        return plainText();
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    /**
     * Makes sure a run boundary falls at {@code offset}, splitting a run if needed.
     *
     * @param offset 0 to {@code length()}, already checked
     * @return the index of the first run that starts at or after {@code offset}
     */
    private int splitAt0(int offset) {
        int pos = 0;
        for (int i = 0; i < runs.size(); i++) {
            TextRun r = runs.get(i);
            if (offset == pos) {
                return i;
            }
            if (offset < pos + r.length()) {
                int cut = offset - pos;
                runs.set(i, r.withText(r.text.substring(0, cut)));
                runs.add(i + 1, r.withText(r.text.substring(cut)));
                return i + 1;
            }
            pos += r.length();
        }
        return runs.size();
    }

    /** Drops empty runs and merges neighbours with equal formatting. */
    private void normalize() {
        List<TextRun> out = new ArrayList<>(runs.size());
        for (TextRun r : runs) {
            if (r.text.isEmpty()) {
                continue;
            }
            int last = out.size() - 1;
            if (last >= 0 && out.get(last).format.equals(r.format)) {
                out.set(last, out.get(last).withText(out.get(last).text + r.text));
            } else {
                out.add(r);
            }
        }
        runs.clear();
        runs.addAll(out);
    }

    private void checkOffset(int offset) {
        int len = length();
        if (offset < 0 || offset > len) {
            throw new EditException("offset " + offset + " is outside the text (0-" + len
                    + ")");
        }
    }

    private void checkRange(int start, int end) {
        if (start > end) {
            throw new EditException("start " + start + " is after end " + end);
        }
        checkOffset(start);
        checkOffset(end);
    }
}