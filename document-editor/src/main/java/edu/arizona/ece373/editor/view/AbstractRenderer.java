package edu.arizona.ece373.editor.view;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentWriter;
import edu.arizona.ece373.editor.model.FormattedText;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shared rendering logic (Template Method pattern): walks the tree as a
 * {@link DocumentWriter}, numbers headings and wraps paragraphs. Subclasses only decide how
 * a piece of text becomes words, in {@link #tokens(FormattedText)}.
 *
 * <p>Output format:
 * <ul>
 *   <li>Each section heading is its number ({@code 1}, {@code 1.2}, {@code 1.2.3}), a space,
 *       then the title's words. The untitled root (level 0) prints nothing.</li>
 *   <li>Each paragraph's words are wrapped by the line breaker at the width. A paragraph
 *       with no words prints as one empty line.</li>
 *   <li>Headings and paragraphs are separated by one blank line. Lines end with {@code \n}
 *       except the last.</li>
 *   <li>A document with no sections and no paragraphs prints {@value #EMPTY}.</li>
 * </ul>
 * Headings are wrapped by the same breaker, so no line is wider than the width unless it is a
 * single word that is longer than the width.
 *
 * <p>Not thread-safe: one render at a time per instance.
 *
 * <p>Satisfies 6.6.1, 6.6.2, 6.2.3. Design doc Fig 2b, Fig 6; Section 5.4.
 */
public abstract class AbstractRenderer implements Renderer, DocumentWriter {

    /** What an empty document renders as. */
    public static final String EMPTY = "(empty document)";

    private final List<String> lines = new ArrayList<>();
    private final List<Integer> numbers = new ArrayList<>();
    private LineBreaker breaker;
    private int width;

    @Override
    public final String render(Document d, LineBreaker b, int width) {
        this.breaker = Objects.requireNonNull(b, "breaker");
        this.width = width;
        lines.clear();
        numbers.clear();
        if (d.root().children().isEmpty()) {
            return EMPTY;
        }
        d.root().writeTo(this, 0);
        return String.join("\n", lines);
    }

    /** Prints the section number and title; level 0 (the root) prints nothing. */
    @Override
    public final void heading(FormattedText t, int level) {
        if (level <= 0) {
            return;
        }
        while (numbers.size() > level) {
            numbers.remove(numbers.size() - 1);  // leaving deeper sections
        }
        if (numbers.size() == level) {
            numbers.set(level - 1, numbers.get(level - 1) + 1);  // next sibling
        } else {
            while (numbers.size() < level) {
                numbers.add(1);  // first child at this level
            }
        }
        List<String> words = new ArrayList<>();
        words.add(sectionNumber());
        words.addAll(tokens(t));
        block(words);
    }

    /** Wraps the paragraph's words at the width. */
    @Override
    public final void paragraph(FormattedText t) {
        block(tokens(t));
    }

    /**
     * Splits text into the words to be wrapped. Each returned word is placed whole on a line
     * and counts its full length toward the width.
     *
     * @param t the text of a paragraph or section title
     * @return its words, in order; empty if it has none
     */
    protected abstract List<String> tokens(FormattedText t);

    private void block(List<String> words) {
        if (!lines.isEmpty()) {
            lines.add("");
        }
        List<String> broken = breaker.breakLines(words, width);
        if (broken.isEmpty()) {
            lines.add("");
        } else {
            lines.addAll(broken);
        }
    }

    private String sectionNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numbers.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(numbers.get(i));
        }
        return sb.toString();
    }
}
