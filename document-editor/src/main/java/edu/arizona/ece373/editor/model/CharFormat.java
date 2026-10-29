/*
 * CharFormat.java   (class (immutable value))
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 2
 * Satisfies: 6.5.1
 * Design doc: Fig 2b; Section 5.6
 *
 * PURPOSE: The formatting of a character: bold, italic, font, size, color.
 *
 * MEMBERS (from the class diagram):
 *   - +bold : boolean
 *   - +italic : boolean
 *   - +font : String
 *   - +size : int
 *   - +color : String
 *   - +with(c : FormatChange) : CharFormat
 *
 * WHAT TO DO:
 *   1. All fields final. Provide a DEFAULT constant (not bold, not italic, a default font, size
 *      12, black).
 *   2. with(change) returns a NEW CharFormat with one attribute changed.
 *   3. Implement equals and hashCode - FormattedText needs them to merge runs, and FormatPool
 *      needs them to share values.
 *
 * TESTED BY: CharFormatTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
