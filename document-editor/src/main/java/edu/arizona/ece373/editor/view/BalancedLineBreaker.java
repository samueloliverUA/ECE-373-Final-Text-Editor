package edu.arizona.ece373.editor.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimum-raggedness line breaking: chooses the breaks that minimise the sum of
 * {@code (width - lineLength)^2} over every line except the last, which makes line lengths
 * more even than {@link GreedyLineBreaker}.
 *
 * <p>Dynamic programming from the last word backwards: {@code best[i]} is the lowest cost of
 * laying out words {@code i..n-1}. For each {@code i} only lines that fit are tried, so the
 * work is {@code O(n * k)} where {@code k} is the most words that fit on one line; that keeps
 * a 100,000-character document fast (6.9.8).
 *
 * <p>Long-word rule (same as greedy): a word longer than the width goes alone on its own line
 * and costs nothing. On ties the break giving a shorter first line wins.
 *
 * <p>Satisfies 6.6.6, 6.9.8. Design doc Section 5.4.
 */
public final class BalancedLineBreaker implements LineBreaker {

    /** Creates a balanced line breaker. */
    public BalancedLineBreaker() {
    }

    @Override
    public List<String> breakLines(List<String> words, int width) {
        int n = words.size();
        long[] best = new long[n + 1];   // best[n] = 0: nothing left to lay out
        int[] next = new int[n + 1];     // next[i] = index of the first word on the next line

        for (int i = n - 1; i >= 0; i--) {
            best[i] = Long.MAX_VALUE;
            int length = -1;
            for (int j = i; j < n; j++) {
                length += 1 + words.get(j).length();
                if (length > width && j > i) {
                    break; // adding word j overflows; longer lines would too
                }
                long cost;
                if (j == n - 1) {
                    cost = 0; // the last line is free
                } else if (length > width) {
                    cost = 0; // a lone over-long word: unavoidable, so no penalty
                } else {
                    long slack = width - length;
                    cost = slack * slack;
                }
                long total = cost + best[j + 1];
                if (total < best[i]) {
                    best[i] = total;
                    next[i] = j + 1;
                }
            }
        }

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < n; i = next[i]) {
            lines.add(String.join(" ", words.subList(i, next[i])));
        }
        return lines;
    }
}
