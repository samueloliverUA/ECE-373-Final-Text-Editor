/*
 * DocumentStatistics.java   (class)
 * Package:   edu.arizona.ece373.editor.view
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 9
 * Pattern:   Observer (concrete observer)
 * Satisfies: 6.7.1, 6.7.2, 6.7.3
 * Design doc: Fig 2b, Fig 3; Section 5.5
 *
 * PURPOSE: Word, character and paragraph counts that stay current without editing commands
 *   computing them.
 *
 * MEMBERS (from the class diagram):
 *   - -words : int
 *   - -chars : int
 *   - -paragraphs : int
 *   - -stale : boolean
 *   - +current() : Statistics
 *
 * WHAT TO DO:
 *   1. Implement DocumentListener: documentChanged only sets stale = true (no counting there).
 *   2. current(): if stale, recount from root.collectParagraphs(), cache, clear stale; return a
 *      Statistics.
 *   3. Decide and document: words = whitespace-separated tokens; characters include spaces;
 *      titles not counted.
 *
 * TESTED BY: DocumentStatisticsTest
 *
 * NOTE: Lives in Sam's view package but is Jack's (statistics subsystem in the work plan).
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
