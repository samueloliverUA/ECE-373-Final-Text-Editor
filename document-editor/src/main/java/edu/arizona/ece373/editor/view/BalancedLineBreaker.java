/*
 * BalancedLineBreaker.java   (class)
 * Package:   edu.arizona.ece373.editor.view
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 16
 * Pattern:   Strategy (concrete)
 * Satisfies: 6.6.6, 6.9.8
 * Design doc: Section 5.4 (second line breaker)
 *
 * PURPOSE: Minimum-raggedness breaking: makes line lengths more even than greedy.
 *
 * WHAT TO DO:
 *   1. Dynamic programming over the words of one paragraph: minimise the sum of (width -
 *      lineLength)^2 over every line EXCEPT the last.
 *   2. Same long-word rule as greedy.
 *   3. Must stay fast on a 100,000-character document (6.9.8): only consider lines that fit.
 *
 * TESTED BY: BalancedLineBreakerTest
 *
 * DONE WHEN: There is a test paragraph where greedy and balanced give different output, and
 *   balanced's is more even.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
