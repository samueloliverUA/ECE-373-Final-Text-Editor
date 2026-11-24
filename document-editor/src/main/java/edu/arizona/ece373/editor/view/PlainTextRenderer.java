package edu.arizona.ece373.editor.view;

import edu.arizona.ece373.editor.model.FormattedText;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain view: words only, no formatting markers. Words are the text split on whitespace, so
 * runs of spaces collapse to one when wrapped.
 *
 * <p>Satisfies 6.6.1.
 */
public final class PlainTextRenderer extends AbstractRenderer {

    /** Creates a renderer that shows words only. */
    public PlainTextRenderer() {
    }

    @Override
    protected List<String> tokens(FormattedText t) {
        List<String> out = new ArrayList<>();
        for (String word : t.plainText().split("\\s+")) {
            if (!word.isEmpty()) {
                out.add(word);
            }
        }
        return out;
    }
}
