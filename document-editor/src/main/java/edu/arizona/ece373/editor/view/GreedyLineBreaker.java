package edu.arizona.ece373.editor.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Fills each line with as many words as fit, then starts the next one. Fast (one pass) but
 * can leave very uneven line lengths; see {@link BalancedLineBreaker}.
 *
 * <p>Satisfies 6.6.5.
 */
public final class GreedyLineBreaker implements LineBreaker {

    /** Creates a greedy line breaker. */
    public GreedyLineBreaker() {
    }

    @Override
    public List<String> breakLines(List<String> words, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= width) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
