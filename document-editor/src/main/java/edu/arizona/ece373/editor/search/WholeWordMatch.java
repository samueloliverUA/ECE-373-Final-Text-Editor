package edu.arizona.ece373.editor.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches the query only when it is a whole word, case-sensitive (6.8.1). Pattern:
 * Strategy (concrete).
 *
 * <p>A match counts only if the character just before it and the character just after
 * it are not letters or digits (or the match is at the start or end of the text). So
 * "program" matches in "a program." and "(program)" but not in "programs" or
 * "reprogram".
 *
 * <p>Anything that is not a letter or digit counts as a boundary, including
 * punctuation, spaces and the underscore: "cat" matches in "cat_food".
 *
 * <p>Matches do not overlap: after a match, the search continues from its end.
 */
public final class WholeWordMatch implements MatchStrategy {

    /** Creates a whole-word match strategy. */
    public WholeWordMatch() {
    }

    @Override
    public List<int[]> findIn(String text, String q) {
        List<int[]> matches = new ArrayList<>();
        if (text == null || q == null || q.isEmpty()) {
            return matches;
        }
        int at = text.indexOf(q);
        while (at >= 0) {
            int end = at + q.length();
            if (isBoundary(text, at - 1) && isBoundary(text, end)) {
                matches.add(new int[] {at, end});
                at = text.indexOf(q, end);      // continue after the match: no overlaps
            } else {
                at = text.indexOf(q, at + 1);   // not a whole word: try one character later
            }
        }
        return matches;
    }

    /** @return true if position i is outside the text or holds a non-letter, non-digit */
    private static boolean isBoundary(String text, int i) {
        if (i < 0 || i >= text.length()) {
            return true;
        }
        return !Character.isLetterOrDigit(text.charAt(i));
    }
}
