package edu.arizona.ece373.editor.search;

import edu.arizona.ece373.editor.model.TextRange;
import java.util.Objects;

/**
 * One search result: where the match is in the document (design doc Figure 2a).
 * Immutable.
 *
 * <p>{@link #toString()} prints the same syntax the user types, e.g. {@code p2:31-38},
 * so a find result can be copied straight into a delete or format command.
 */
public final class Match {

    /** The matched characters: paragraph number, start, end (exclusive). */
    public final TextRange range;

    /**
     * Creates a match at a text range.
     *
     * @param range where the match is; must not be null
     */
    public Match(TextRange range) {
        this.range = Objects.requireNonNull(range, "range");
    }

    /** Equal when both matches cover the same paragraph, start and end. */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Match)) {
            return false;
        }
        Match m = (Match) o;
        return m.range.paragraph == range.paragraph
                && m.range.start == range.start
                && m.range.end == range.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(range.paragraph, range.start, range.end);
    }

    /** @return the range in the form the user types, e.g. "p2:31-38" */
    @Override
    public String toString() {
        return range.toString();
    }
}
