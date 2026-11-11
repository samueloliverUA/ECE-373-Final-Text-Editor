/*
 * CommandParser.java   (class)
 * Package:   edu.arizona.ece373.editor.cli
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Pattern:   Factory (table of ActionFactory objects keyed by verb)
 * Satisfies: 6.1.1, 6.1.3, 6.1.5
 * Design doc: Section 1.3 (command table), Fig 2a, Fig 3
 *
 * PURPOSE: Turns one line of text into an Action. The ONLY class that knows command syntax.
 *
 * MEMBERS (from the class diagram):
 *   - -factories : Map<String, ActionFactory>
 *   - +parse(line : String) : Action
 *
 * WHAT TO DO:
 *   1. Split the line into tokens: words, quoted strings ("two words"), positions p2:14, ranges
 *      p2:5-12, section paths 2.1.
 *   2. Look up the verb in the factories map. Some verbs are two words: 'section add', 'para
 *      add', 'checkpoint save', 'checkpoint restore', 'checkpoint list', 'macro record', 'macro
 *      end', 'macro play', 'format title'.
 *   3. In the constructor register one ActionFactory (a lambda) per verb. Adding a command =
 *      adding one entry.
 *   4. Unknown verb, missing argument or bad number -> throw ParseException with a one-line
 *      message that names the problem.
 *   5. Never touch the Document here. Build an Action that calls Editor methods when run.
 *   6. Week 1: section add, para add, outline, undo, redo, quit. Add the rest of the verbs
 *      (Section 1.3 table of the design doc) in the week each feature lands.
 *
 * TESTED BY: CommandParserTest
 *
 * DONE WHEN: Every command in the Section 1.3 table parses, and every malformed line produces a
 *   clear ParseException.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
