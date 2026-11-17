package edu.arizona.ece373.editor.view;

import java.util.List;

/**
 * Splits a list of words into lines no wider than the width (Strategy pattern). Words on a
 * line are separated by one space. A word longer than the width is never split: it goes alone
 * on its own line, which is then the only line allowed to exceed the width.
 *
 * <p>Satisfies 6.6.5-6.6.7, 6.9.3. Design doc Fig 2b; Section 5.4.
 */
public interface LineBreaker {

    /**
     * @param words the words of one paragraph, in order; none contains a line break
     * @param width the maximum line length in characters, at least 1
     * @return the lines, in order; empty if {@code words} is empty
     */
    List<String> breakLines(List<String> words, int width);
}
