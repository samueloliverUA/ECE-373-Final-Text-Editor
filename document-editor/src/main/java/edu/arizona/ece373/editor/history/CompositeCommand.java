/*
 * CompositeCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Oct 28
 * Pattern:   Composite + Command
 * Satisfies: 6.8.4, 6.8.7
 * Design doc: Fig 2a, Figs 4, 5, 7; Section 5.3
 *
 * PURPOSE: A list of EditCommands that acts as one: Replace All and macro replays.
 *
 * MEMBERS (from the class diagram):
 *   - -parts : List<EditCommand>
 *   - +add(c : EditCommand)
 *
 * WHAT TO DO:
 *   1. Implement EditCommand. add(c) appends a part.
 *   2. validate each part; execute parts in order; undo parts in REVERSE order.
 *
 * TESTED BY: HistoryTest, ReplaceAllTest, MacroLibraryTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
