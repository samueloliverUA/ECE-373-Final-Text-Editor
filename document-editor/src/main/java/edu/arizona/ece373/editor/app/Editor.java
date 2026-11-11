/*
 * Editor.java   (class (facade))
 * Package:   edu.arizona.ece373.editor.app
 * Owner:     Shared        Reviewer: Both
 * When:      Week of Oct 28
 * Pattern:   Facade
 * Satisfies: 6.1.6, 6.9.6, 6.9.10
 * Design doc: Fig 2a; Section 5.9
 *
 * PURPOSE: The single entry point into the engine and the composition root: it creates and
 *   wires every engine and view object.
 *
 * MEMBERS (from the class diagram):
 *   - +perform(cmd : EditCommand) : Result
 *   - +undo() : Result
 *   - +redo() : Result
 *   - +saveCheckpoint(name : String)
 *   - +restoreCheckpoint(name : String) : Result
 *   - +find(q : String, m : MatchStrategy) : List<Match>
 *   - +replaceAll(q, r : String, m : MatchStrategy) : Result
 *   - +render() : String
 *   - +stats() : Statistics
 *
 * WHAT TO DO:
 *   1. Sam writes the skeleton in week 1: constructor that creates Document, History,
 *      CheckpointStore, SearchService, MacroLibrary, ViewSettings and DocumentStatistics, and
 *      registers DocumentStatistics as a listener on Document.
 *   2. Each owner then adds the methods for their own subsystem the week that subsystem lands
 *      (Jack: perform/undo/redo/checkpoints/find/replaceAll/stats; Sam: render/view
 *      settings/macros).
 *   3. Every public method returns a Result. Catch EditException and turn it into
 *      Result.error(message) - nothing escapes to the console layer.
 *   4. No console input or output in this class. No singletons, no static state.
 *   5. Add getters other layers need: document(), viewSettings(), macros(), history().
 *
 * TESTED BY: EditorTest, EditorErrorTest
 *
 * NOTE: Shared file: merge conflicts are likely. Each of you edits only your own methods, in
 *   separate pull requests.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
