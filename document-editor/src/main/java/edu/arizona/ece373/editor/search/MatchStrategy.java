/*
 * MatchStrategy.java   (interface)
 * Package:   edu.arizona.ece373.editor.search
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 16
 * Pattern:   Strategy
 * Satisfies: 6.8.1
 * Design doc: Fig 2a; Section 5.4
 *
 * PURPOSE: How a query matches inside one string: substring or whole word.
 *
 * MEMBERS (from the class diagram):
 *   - +findIn(text : String, q : String) : List<int[]>
 *
 * WHAT TO DO:
 *   1. Declare findIn(text, q) : List<int[]> of {start, end} pairs, left to right, non-
 *      overlapping.
 *
 * TESTED BY: SubstringMatchTest, WholeWordMatchTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
