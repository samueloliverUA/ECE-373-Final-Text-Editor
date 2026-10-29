/*
 * ViewSettings.java   (class)
 * Package:   edu.arizona.ece373.editor.view
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 9
 * Satisfies: 6.6.3, 6.6.4, 6.6.7
 * Design doc: Fig 2b, Fig 6
 *
 * PURPOSE: Current view (plain or markup), line breaker (greedy or balanced) and width.
 *
 * MEMBERS (from the class diagram):
 *   - -width : int
 *   - +setWidth(w : int)
 *   - +renderer() : Renderer
 *   - +breaker() : LineBreaker
 *   - +width() : int
 *
 * WHAT TO DO:
 *   1. Defaults: PlainTextRenderer, GreedyLineBreaker, width 80.
 *   2. setWidth(w): reject outside 10-200 and keep the old width.
 *   3. setRenderer(r), setBreaker(b), plus getters. These changes are NOT undoable (team
 *      exclusion).
 *
 * TESTED BY: ViewSettingsTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
