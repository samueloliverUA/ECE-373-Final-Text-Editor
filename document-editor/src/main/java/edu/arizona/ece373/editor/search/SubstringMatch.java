package edu.arizona.ece373.editor.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches the query anywhere in the text, case-sensitive (6.8.1). Pattern: Strategy
 * (concrete). For example, "cat" matches inside "concatenate".
 *
 * <p>Matches do not overlap: after a match, the search continues from its end, so
 * "aa" in "aaaa" gives {0, 2} and {2, 4}.
 */
public final class SubstringMatch implements MatchStrategy {

    /** Creates a substring match strategy. */
    public SubstringMatch() {
    }

    @Override
    public List<int[]> findIn(String text, String q) {
        List<int[]> matches = new ArrayList<>();
        if (text == null || q == null || q.isEmpty()) {
            return matches;
        }
        int from = 0;
        int at = text.indexOf(q, from);
        while (at >= 0) {
            matches.add(new int[] {at, at + q.length()});
            from = at + q.length(); // continue after this match, so matches never overlap
            at = text.indexOf(q, from);
        }
        return matches;
    }
}
