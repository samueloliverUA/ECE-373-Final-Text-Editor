package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Section;
import edu.arizona.ece373.editor.model.SectionPath;

/** Adds a section, top-level or under a parent path, with a title (6.2.1, 6.2.2). */
public final class AddSectionCommand implements EditCommand {

    private final SectionPath parentPath;
    private final String title;
    private Section added;
    private SectionPath addedPath;

    /**
     * Creates a command that adds a section.
     *
     * @param parentPath the parent section, or null for a top-level section
     * @param title the title; must not be blank
     */
    public AddSectionCommand(SectionPath parentPath, String title) {
        this.parentPath = parentPath;
        this.title = title;
    }

    @Override
    public void validate(Document d) {
        if (title == null || title.isBlank()) {
            throw new EditException("Section title cannot be empty");
        }
        d.section(parentPath); // throws "No section 2.4"
    }

    @Override
    public void execute(Document d) {
        Section parent = d.section(parentPath);
        if (added == null) {
            added = new Section(new FormattedText(title));
        }
        parent.add(added);
        addedPath = SectionPath.child(parentPath, parent.subsections().size());
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    @Override
    public void undo(Document d) {
        // Look the nodes up again: a checkpoint restore since execute may have replaced them.
        d.section(parentPath).remove(d.section(addedPath));
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
    }

    /**
     * Returns the path of the section this command added.
     *
     * @return the path the section was added at, or null before {@link #execute}
     */
    public SectionPath addedPath() {
        return addedPath;
    }

    @Override
    public String describe() {
        return addedPath == null
                ? "add section \"" + title + "\""
                : "added section " + addedPath + " \"" + title + "\"";
    }
}
