/*
 * AbstractRenderer.java   (abstract class)
 * Package:   edu.arizona.ece373.editor.view
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 9
 * Pattern:   Template Method
 * Satisfies: 6.6.1, 6.6.2, 6.2.3
 * Design doc: Fig 2b, Fig 6; Section 5.4
 *
 * PURPOSE: Shared rendering logic: walks the tree as a DocumentWriter, numbers headings, wraps
 *   paragraphs.
 *
 * MEMBERS (from the class diagram):
 *   - #tokens(t : FormattedText) : List<String>  (abstract)
 *
 * WHAT TO DO:
 *   1. Implement Renderer and DocumentWriter.
 *   2. render(): reset output, d's root.writeTo(this, 0), return the text. Empty document ->
 *      '(empty document)'.
 *   3. heading(t, level): print a number like 1, 1.2, 1.2.3 then the title (root level 0 prints
 *      nothing).
 *   4. paragraph(t): tokens(t) -> breaker.breakLines(tokens, width) -> append lines, blank line
 *      between paragraphs.
 *   5. Subclasses only implement tokens(t).
 *
 * TESTED BY: PlainTextRendererTest, MarkupRendererTest
 *
 * NOTE: This is a Template Method. It is not named in the design rationale; worth mentioning in
 *   the Q&A.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
