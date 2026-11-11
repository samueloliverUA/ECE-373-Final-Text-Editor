/*
 * Document.java   (class)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Satisfies: 6.1.6, 6.2.1
 * Design doc: Fig 2b; Section 5.1
 *
 * PURPOSE: The single document: owns the untitled root Section, finds paragraphs and sections,
 *   and notifies listeners.
 *
 * MEMBERS (from the class diagram):
 *   - -listeners : List<DocumentListener>
 *   - +paragraph(n : int) : Paragraph
 *   - +section(p : SectionPath) : Section
 *   - +addListener(l : DocumentListener)
 *   - +fireChanged(e : DocumentEvent)
 *   - +createMemento() : DocumentMemento
 *   - +restore(m : DocumentMemento)
 *
 * WHAT TO DO:
 *   1. Hold one root Section with an empty title.
 *   2. paragraph(n): paragraphs are numbered 1, 2, 3... in document order across all sections
 *      (use root.collectParagraphs). Missing -> EditException.
 *   3. section(path): walk the SectionPath from the root. Missing -> EditException ("No section
 *      2.4").
 *   4. addListener(l) and fireChanged(e): keep a List<DocumentListener>; fireChanged calls each
 *      in order.
 *   5. createMemento() / restore(m): JACK adds these in week 3 for checkpoints (deep copy of
 *      the root via deepCopy).
 *
 * TESTED BY: DocumentTest
 *
 * NOTE: Sam owns this file; Jack adds createMemento/restore in his checkpoint pull request
 *   (week of Nov 9).
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
