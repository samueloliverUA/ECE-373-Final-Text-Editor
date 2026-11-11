/*
 * TextRange.java   (class (value))
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Satisfies: 6.1.5
 * Design doc: Fig 2b
 *
 * PURPOSE: A character range inside one paragraph: paragraph number, start, end (end
 *   exclusive).
 *
 * MEMBERS (from the class diagram):
 *   - +paragraph : int
 *   - +start : int
 *   - +end : int
 *
 * WHAT TO DO:
 *   1. Fields paragraph (1-based), start, end; immutable.
 *   2. Validation helper: check against the Document (paragraph exists, 0 <= start <= end <=
 *      length) -> EditException otherwise.
 *   3. toString() prints p2:5-12 so find results print the same syntax the user types.
 *
 * TESTED BY: each *CommandTest, SubstringMatchTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
