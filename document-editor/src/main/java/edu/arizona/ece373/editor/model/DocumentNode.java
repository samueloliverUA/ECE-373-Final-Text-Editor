/*
 * DocumentNode.java   (abstract class)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Pattern:   Composite (Component)
 * Satisfies: 6.2.3
 * Design doc: Fig 2b; Section 5.1
 *
 * PURPOSE: Common type for Section and Paragraph so every operation works at any nesting depth.
 *
 * MEMBERS (from the class diagram):
 *   - +writeTo(w : DocumentWriter, depth : int)
 *   - +collectParagraphs(out : List<Paragraph>)
 *   - +deepCopy() : DocumentNode
 *
 * WHAT TO DO:
 *   1. Declare abstract writeTo(w, depth), collectParagraphs(out) and deepCopy().
 *   2. Callers hold a DocumentNode and never use instanceof.
 *
 * TESTED BY: DeepNestingTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
