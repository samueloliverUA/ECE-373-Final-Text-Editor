package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRun;
import java.util.List;

/**
 * Applies one formatting change to a whole section title, e.g.
 * {@code format title 2.1 size 18}. Works like {@link FormatTextCommand}, on
 * {@code section.title()} over its full length: the characters never change, and undo puts
 * the old runs back exactly.
 *
 * <p>As with FormatTextCommand, the change may be given built or as raw
 * {@code {attribute, value}} words, which {@link #validate} checks.
 *
 * <p>Satisfies 6.5.5. Design doc UC3 alternate flow 1a.
 */
public final class FormatTitleCommand implements EditCommand {

    private final SectionPath section;
    private final String[] raw;
    private FormatChange change;
    private List<TextRun> oldRuns;

    /**
     * Creates a command that formats a section title.
     *
     * @param section the section whose title to format
     * @param change the attribute and its new value
     */
    public FormatTitleCommand(SectionPath section, FormatChange change) {
        this.section = section;
        this.change = change;
        this.raw = null;
    }

    /**
     * Creates a command that formats a section title.
     *
     * @param section the section whose title to format
     * @param attrAndValue the attribute and value as typed, e.g. {@code {"bold", "on"}};
     *     checked by {@link #validate}
     */
    public FormatTitleCommand(SectionPath section, String[] attrAndValue) {
        this.section = section;
        this.raw = attrAndValue.clone();
    }

    @Override
    public void validate(Document d) {
        if (section == null) {
            throw new EditException("format title needs a section like 2.1");
        }
        if (change == null) {
            change = FormatChange.parse(raw); // throws on a bad attribute or value
        }
        d.section(section); // throws "No section 2.4"
    }

    @Override
    public void execute(Document d) {
        FormattedText title = d.section(section).title();
        oldRuns = title.applyFormat(0, title.length(), change);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.FORMAT));
    }

    @Override
    public void undo(Document d) {
        d.section(section).title().restoreRuns(oldRuns);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.FORMAT));
    }

    @Override
    public String describe() {
        return "formatted title of section " + section + " "
                + (change != null ? change.toString() : String.join(" ", raw));
    }
}
