/*
 * DocumentWriter.java   (interface)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 9
 * Satisfies: 6.2.3, 6.6.1, 6.6.2
 * Design doc: Fig 2b, Fig 6
 *
 * PURPOSE: Callback the document tree writes itself into; renderers implement it.
 *
 * MEMBERS (from the class diagram):
 *   - +heading(t : FormattedText, level : int)
 *   - +paragraph(t : FormattedText)
 *
 * WHAT TO DO:
 *   1. Declare heading(t, level) and paragraph(t).
 *   2. The tree decides the ORDER; the writer decides the LOOK (numbering, wrapping, markup).
 *
 * TESTED BY: PlainTextRendererTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
