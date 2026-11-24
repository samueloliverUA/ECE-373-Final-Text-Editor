package edu.arizona.ece373.editor.view;

/**
 * An immutable snapshot of the document's word, character and paragraph counts
 * (6.7.1-6.7.3), returned by {@code Editor.stats()}.
 */
public final class Statistics {

    private final int words;
    private final int characters;
    private final int paragraphs;

    /**
     * Creates a snapshot of the three counts.
     *
     * @param words whitespace-separated words in all paragraphs
     * @param characters characters in all paragraphs, spaces included
     * @param paragraphs number of paragraphs
     */
    public Statistics(int words, int characters, int paragraphs) {
        this.words = words;
        this.characters = characters;
        this.paragraphs = paragraphs;
    }

    /**
     * Returns the word count.
     *
     * @return the word count
     */
    public int words() {
        return words;
    }

    /**
     * Returns the character count, spaces included.
     *
     * @return the character count, spaces included
     */
    public int characters() {
        return characters;
    }

    /**
     * Returns the paragraph count.
     *
     * @return the paragraph count
     */
    public int paragraphs() {
        return paragraphs;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Statistics)) {
            return false;
        }
        Statistics s = (Statistics) o;
        return s.words == words && s.characters == characters && s.paragraphs == paragraphs;
    }

    @Override
    public int hashCode() {
        return (words * 31 + characters) * 31 + paragraphs;
    }

    /** @return the 'stats' command output, e.g. "Words: 2, characters: 11, paragraphs: 1" */
    @Override
    public String toString() {
        return "Words: " + words + ", characters: " + characters + ", paragraphs: " + paragraphs;
    }
}
