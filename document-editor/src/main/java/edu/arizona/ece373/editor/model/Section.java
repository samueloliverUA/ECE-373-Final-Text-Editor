/*
 * Section.java   (class)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Pattern:   Composite
 * Satisfies: 6.2.1, 6.2.2, 6.2.3
 * Design doc: Fig 2b, Fig 6; Section 5.1
 *
 * PURPOSE: A titled container of paragraphs and subsections, to any depth.
 *
 * MEMBERS (from the class diagram):
 *   - -children : List<DocumentNode>
 *   - +add(index : int, n : DocumentNode)
 *   - +remove(n : DocumentNode)
 *
 * WHAT TO DO:
 *   1. Hold a title (FormattedText) and an ordered List<DocumentNode> children.
 *   2. add(index, n) and remove(n); also indexOf(n) so split/merge can find a paragraph's
 *      neighbour.
 *   3. writeTo: w.heading(title, depth), then each child.writeTo(w, depth + 1).
 *   4. collectParagraphs: ask each child in order. deepCopy: copy title and every child.
 *
 * TESTED BY: SectionTest, DeepNestingTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
