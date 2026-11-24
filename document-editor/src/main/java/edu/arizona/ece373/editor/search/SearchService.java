package edu.arizona.ece373.editor.search;

import edu.arizona.ece373.editor.history.CompositeCommand;
import edu.arizona.ece373.editor.history.ReplaceTextCommand;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.TextRange;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Finds matches across the whole document and builds the commands that replace them
 * (6.8.1-6.8.4; design doc Figure 2a, Figures 4 and 5).
 *
 * <p><b>Decision: only paragraph text is searched, not section titles,</b> as in the
 * design doc. Titles are not edited with the text commands, so a title match could
 * not be replaced the same way.
 *
 * <p>This class never changes the document. It returns matches, and commands for the
 * caller (the Editor) to run through {@code History}, so a Replace All is one undo.
 */
public final class SearchService {

    /** Creates a search service. It holds no state. */
    public SearchService() {
    }

    /**
     * Finds every match in the document, in document order.
     *
     * @param d the document
     * @param q the query; must not be empty
     * @param m how the query matches (substring or whole word)
     * @return the matches, paragraph by paragraph and left to right; empty if none
     * @throws EditException if the query is null or empty
     */
    public List<Match> findAll(Document d, String q, MatchStrategy m) {
        if (q == null || q.isEmpty()) {
            throw new EditException("Search text cannot be empty");
        }
        List<Paragraph> paragraphs = new ArrayList<>();
        d.section(null).collectParagraphs(paragraphs);

        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < paragraphs.size(); i++) {
            String text = paragraphs.get(i).text().plainText();
            for (int[] found : m.findIn(text, q)) {
                // Paragraph numbers start at 1.
                matches.add(new Match(new TextRange(i + 1, found[0], found[1])));
            }
        }
        return matches;
    }

    /**
     * Builds one command that replaces every match, so Replace All undoes in one step.
     *
     * <p>The parts are added from the LAST match to the FIRST (Figure 4). Each
     * replacement then only moves text after itself, so every range found by
     * {@link #findAll} is still correct when its part runs.
     *
     * <p>With no matches the result is an EMPTY CompositeCommand. The caller must check
     * {@link CompositeCommand#isEmpty()} and not perform it, so nothing is added to the
     * history.
     *
     * @param matches the matches from {@link #findAll}
     * @param r the replacement text; null is treated as empty
     * @return one CompositeCommand with a ReplaceTextCommand per match
     */
    public CompositeCommand replaceAllCommand(List<Match> matches, String r) {
        String replacement = r;
        if (replacement == null) {
            replacement = "";
        }
        List<Match> lastToFirst = new ArrayList<>(matches);
        // Sort so it works even if the caller's list is not in document order.
        lastToFirst.sort(Comparator.comparingInt((Match x) -> x.range.paragraph)
                .thenComparingInt(x -> x.range.start)
                .reversed());

        CompositeCommand all = new CompositeCommand("replace all with \"" + replacement + "\"");
        for (Match match : lastToFirst) {
            all.add(new ReplaceTextCommand(match.range, replacement));
        }
        return all;
    }

    /**
     * Builds the command for replace-first: one replacement of one match.
     *
     * @param match the match to replace, usually the first one from {@link #findAll}
     * @param r the replacement text; null is treated as empty
     * @return a ReplaceTextCommand for that match
     */
    public ReplaceTextCommand replaceFirstCommand(Match match, String r) {
        Objects.requireNonNull(match, "match");
        return new ReplaceTextCommand(match.range, r);
    }
}
