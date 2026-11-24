package edu.arizona.ece373.editor.view;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import java.util.Objects;

/**
 * The current view (plain or markup), line breaker (greedy or balanced) and width. Defaults:
 * plain view, greedy breaker, width {@value #DEFAULT_WIDTH}.
 *
 * <p>These settings change how the document is shown, not the document, so changing them is
 * NOT undoable (team exclusion).
 *
 * <p>Satisfies 6.6.3, 6.6.4, 6.6.7. Design doc Fig 2b, Fig 6.
 */
public final class ViewSettings {

    /** Narrowest allowed width. */
    public static final int MIN_WIDTH = 10;
    /** Widest allowed width. */
    public static final int MAX_WIDTH = 200;
    /** Width used until {@link #setWidth} is called. */
    public static final int DEFAULT_WIDTH = 80;

    private Renderer renderer = new PlainTextRenderer();
    private LineBreaker breaker = new GreedyLineBreaker();
    private int width = DEFAULT_WIDTH;

    /** Creates settings with the default view, line breaker and width. */
    public ViewSettings() {
    }

    /**
     * Sets the line width.
     *
     * @param w the new width, {@value #MIN_WIDTH} to {@value #MAX_WIDTH}
     * @throws EditException if out of range; the old width is kept (UC4 1a)
     */
    public void setWidth(int w) {
        if (w < MIN_WIDTH || w > MAX_WIDTH) {
            throw new EditException("width must be " + MIN_WIDTH + "-" + MAX_WIDTH
                    + ", got " + w + "; width is still " + width);
        }
        width = w;
    }

    /**
     * Sets the renderer.
     *
     * @param r the renderer to use from now on
     */
    public void setRenderer(Renderer r) {
        renderer = Objects.requireNonNull(r, "renderer");
    }

    /**
     * Sets the line breaker.
     *
     * @param b the line breaker to use from now on
     */
    public void setBreaker(LineBreaker b) {
        breaker = Objects.requireNonNull(b, "breaker");
    }

    /**
     * Selects a view by the name the {@code view} command uses.
     *
     * @param name {@code plain} or {@code markup}
     * @throws EditException for any other name; the view is unchanged
     */
    public void setView(String name) {
        switch (String.valueOf(name)) {
            case "plain" -> renderer = new PlainTextRenderer();
            case "markup" -> renderer = new MarkupRenderer();
            default -> throw new EditException("view must be plain or markup, got '"
                    + name + "'");
        }
    }

    /**
     * Selects a line breaker by the name the {@code breaker} command uses.
     *
     * @param name {@code greedy} or {@code balanced}
     * @throws EditException for any other name; the breaker is unchanged
     */
    public void setBreaker(String name) {
        switch (String.valueOf(name)) {
            case "greedy" -> breaker = new GreedyLineBreaker();
            case "balanced" -> breaker = new BalancedLineBreaker();
            default -> throw new EditException("breaker must be greedy or balanced, got '"
                    + name + "'");
        }
    }

    /**
     * Returns the current renderer.
     *
     * @return the current renderer
     */
    public Renderer renderer() {
        return renderer;
    }

    /**
     * Returns the current line breaker.
     *
     * @return the current line breaker
     */
    public LineBreaker breaker() {
        return breaker;
    }

    /**
     * Returns the current width.
     *
     * @return the current width
     */
    public int width() {
        return width;
    }

    /**
     * Renders a document with the current settings.
     *
     * @param d the document
     * @return {@code d} rendered with the current renderer, breaker and width
     */
    public String render(Document d) {
        return renderer.render(d, breaker, width);
    }
}
