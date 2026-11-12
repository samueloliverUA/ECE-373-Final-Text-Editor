package edu.arizona.ece373.editor.model;

/**
 * Anything that wants to hear about document changes (Observer pattern): statistics now,
 * auto-save in a future release. Registered with {@link Document#addListener} and called by
 * {@link Document#fireChanged} after every change, undo and redo.
 *
 * <p>Satisfies 6.7.2, 6.7.3, 6.9.4.
 */
@FunctionalInterface
public interface DocumentListener {

    /**
     * Called after the document has changed.
     *
     * @param e what kind of change happened
     */
    void documentChanged(DocumentEvent e);
}
