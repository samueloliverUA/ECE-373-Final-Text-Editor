/*
 * FormattedText.java   (class)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Satisfies: 6.3.1-6.3.6, 6.5.2, 6.5.3, 6.5.4
 * Design doc: Fig 2b; Section 5.6
 *
 * PURPOSE: A string where different characters carry different formatting, stored as a list of
 *   runs.
 *
 * MEMBERS (from the class diagram):
 *   - -runs : List<TextRun>
 *   - +length() : int
 *   - +plainText() : String
 *   - +insert(off : int, t : FormattedText)
 *   - +cut(start : int, end : int) : FormattedText
 *   - +applyFormat(s : int, e : int, c : FormatChange) : FormattedText
 *   - +splitAt(off : int) : FormattedText
 *   - +append(t : FormattedText)
 *
 * WHAT TO DO:
 *   1. Hold an ordered List<TextRun>. All offsets are character offsets from 0; ranges are
 *      start-inclusive, end-exclusive.
 *   2. length(), plainText().
 *   3. insert(off, t): splice t's runs in at off, keeping t's formatting.
 *   4. cut(start, end): remove the range and RETURN it as a FormattedText (commands keep it for
 *      undo).
 *   5. splitAt(off): keep the left part, return the right part. append(t): add t's runs to the
 *      end.
 *   6. applyFormat(s, e, change) (week 2): split runs at s and e, apply change to the runs
 *      inside, merge equal neighbours; return the old runs for undo.
 *   7. Bad offsets -> EditException. Never leave empty runs behind.
 *
 * TESTED BY: FormattedTextTest
 *
 * DONE WHEN: A format range that crosses three existing runs produces the right runs and
 *   identical plainText().
 *
 * NOTE: Most-used class in the project. Get insert/cut/splitAt/append solid in week 1; Jack's
 *   editing commands depend on them.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
