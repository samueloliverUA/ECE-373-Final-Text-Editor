/*
 * SearchService.java   (class)
 * Package:   edu.arizona.ece373.editor.search
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 16
 * Satisfies: 6.8.1-6.8.4, 6.2.3
 * Design doc: Fig 2a, Figs 4-5
 *
 * PURPOSE: Finds matches across the whole document and builds the replace commands.
 *
 * MEMBERS (from the class diagram):
 *   - +findAll(d : Document, q : String, m : MatchStrategy) : List<Match>
 *   - +replaceAllCommand(...) : CompositeCommand
 *
 * WHAT TO DO:
 *   1. findAll(d, q, m): for each paragraph in document order, m.findIn(text, q) -> Match list.
 *      Empty q -> error.
 *   2. replaceAllCommand(matches, r): one CompositeCommand of ReplaceTextCommands, created LAST
 *      match to FIRST within each paragraph so earlier offsets stay valid (Figure 4).
 *   3. Add replaceFirstCommand(match, r) for replace-first (one ReplaceTextCommand on the first
 *      match).
 *   4. No matches -> return an empty result so nothing is added to the history.
 *
 * TESTED BY: SearchServiceTest, ReplaceAllTest
 *
 * NOTE: Decide whether section titles are searched (design doc says paragraphs). Write the
 *   decision in JavaDoc.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
