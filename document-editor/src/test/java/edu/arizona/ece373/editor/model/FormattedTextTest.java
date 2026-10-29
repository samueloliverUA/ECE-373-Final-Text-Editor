/*
 * FormattedTextTest.java   (JUnit 5 test class)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Satisfies: 6.3.x, 6.5.2, 6.5.3
 *
 * PURPOSE: Tests FormattedText and TextRun.
 *
 * WHAT TO DO:
 *   1. insert/cut/splitAt/append at start, middle, end and on an empty text.
 *   2. Formatting is kept on inserted and remaining characters.
 *   3. applyFormat across several runs; equal neighbours merge; no empty runs.
 *   4. Bad offsets -> EditException.
 *
 * DONE WHEN: All cases pass under mvn clean test, including the boundary cases.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
