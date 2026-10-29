/*
 * MacroLibrary.java   (class)
 * Package:   edu.arizona.ece373.editor.app
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 16
 * Satisfies: 6.8.5, 6.8.6, 6.8.7
 * Design doc: Fig 2a, Fig 7; Sections 5.3, 5.8
 *
 * PURPOSE: Records Actions under a name and replays them as one undoable history entry.
 *
 * MEMBERS (from the class diagram):
 *   - -macros : Map<String, List<Action>>
 *   - -recording : List<Action>
 *   - +startRecording(name : String)
 *   - +record(a : Action)
 *   - +stopRecording()
 *   - +play(name : String, e : Editor) : Result
 *
 * WHAT TO DO:
 *   1. startRecording(name): error if already recording.
 *   2. record(a): ConsoleApp calls this after an action SUCCEEDS and only if a.recordable().
 *   3. stopRecording(): save the list under the name (overwrite an existing macro with that
 *      name).
 *   4. play(name, e): unknown name -> error. Playing while already playing -> error (no nested
 *      macros).
 *   5. play: call history.beginGroup("macro " + name), run each step in order, then
 *      history.endGroup().
 *   6. If a step fails: history.abortGroup() (rolls the replay back), stop, and report which
 *      step failed (UC6 alt 3b).
 *
 * TESTED BY: MacroLibraryTest
 *
 * DONE WHEN: One undo after 'macro play x' restores the document exactly as it was before the
 *   replay.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
