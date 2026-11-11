package edu.arizona.ece373.editor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A section address such as {@code 2.1}: the second top-level section, then its first
 * subsection. Indices are 1-based. Immutable.
 *
 * <p>Satisfies 6.2.2. Used by {@code section add}, {@code para add} and {@code format title}.
 */
public final class SectionPath {

    private final List<Integer> indices;

    private SectionPath(List<Integer> indices) {
        this.indices = Collections.unmodifiableList(new ArrayList<>(indices));
    }

    /**
     * Parses a dotted path such as {@code "2.1"}.
     *
     * @param text the dotted form
     * @return the path
     * @throws IllegalArgumentException if a part is empty, not a number, zero or negative
     */
    public static SectionPath parse(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("empty section path");
        }
        // split with limit -1 keeps trailing empty parts, so "2." is rejected
        String[] parts = text.split("\\.", -1);
        List<Integer> out = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty() || !part.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("bad section path '" + text + "'");
            }
            int value;
            try {
                value = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad section path '" + text + "'");
            }
            if (value < 1) {
                throw new IllegalArgumentException(
                        "section numbers start at 1 in '" + text + "'");
            }
            out.add(value);
        }
        return new SectionPath(out);
    }

    /**
     * Returns the path of a child of {@code parent}, or a top-level path if parent is null.
     *
     * @param parent the parent path, or null for the root
     * @param index 1-based index of the child among the parent's subsections
     * @return the child path
     */
    public static SectionPath child(SectionPath parent, int index) {
        List<Integer> out = new ArrayList<>(parent == null ? List.of() : parent.indices);
        out.add(index);
        return new SectionPath(out);
    }

    /** @return the number of levels; 1 for a top-level section */
    public int depth() {
        return indices.size();
    }

    /**
     * @param level 0-based level
     * @return the 1-based section index at that level
     */
    public int index(int level) {
        return indices.get(level);
    }

    /** @return the dotted form, e.g. {@code 2.1} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indices.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(indices.get(i));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SectionPath other && indices.equals(other.indices);
    }

    @Override
    public int hashCode() {
        return indices.hashCode();
    }
}
