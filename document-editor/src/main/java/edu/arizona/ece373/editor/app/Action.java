/*
 * Action.java   (interface)
 * Package:   edu.arizona.ece373.editor.app
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Pattern:   Command (UI-level command; EditCommand is the document-level one)
 * Satisfies: 6.8.5
 * Design doc: Fig 2a; Section 5.8 (macros store actions, not text)
 *
 * PURPOSE: One parsed command, ready to run against the Editor. Macros store and replay these.
 *
 * MEMBERS (from the class diagram):
 *   - +run(e : Editor) : Result
 *
 * WHAT TO DO:
 *   1. Declare run(e : Editor) : Result.
 *   2. Suggested addition: default boolean recordable() returning true; undo, redo, macro and
 *      run actions return false so MacroLibrary skips them (UC6 alternate flow 1b).
 *
 * TESTED BY: MacroLibraryTest, CommandParserTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
