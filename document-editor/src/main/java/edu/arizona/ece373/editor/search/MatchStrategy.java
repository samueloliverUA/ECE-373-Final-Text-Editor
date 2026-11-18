package edu.arizona.ece373.editor.search;

import java.util.List;

/**
 * How a query matches inside one string: for example as a plain substring or only as
 * a whole word (6.8.1; design doc Figure 2a, Section 5.4). Pattern: Strategy. The
 * SearchService runs one strategy over every paragraph.
 *
 * <p>Contract every implementation must follow:
 * <ul>
 *   <li>Each match is an {@code int[]} of {start, end}, end exclusive, so
 *       {@code text.substring(start, end)} is the matched text.</li>
 *   <li>Matches are listed left to right and do not overlap: after a match, the search
 *       continues at its end. Searching "aa" in "aaaa" gives {0, 2} and {2, 4}.</li>
 *   <li>Matching is case-sensitive.</li>
 *   <li>An empty or null query, or an empty text, gives an empty list (never null).</li>
 * </ul>
 */
public interface MatchStrategy {

    /**
     * Finds every match of a query in one string.
     *
     * @param text the string to search, e.g. one paragraph's plain text
     * @param q the query
     * @return the {start, end} pairs, left to right and non-overlapping; empty if none
     */
    List<int[]> findIn(String text, String q);
}
