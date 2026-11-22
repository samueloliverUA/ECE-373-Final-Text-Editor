package edu.arizona.ece373.editor.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shares identical {@link CharFormat} objects, so a document with thousands of runs in
 * the same few styles keeps only one object per style (optional requirement 6.5.6;
 * design doc Figure 2b, Sections 1.2 and 5.6). Pattern: Flyweight (factory).
 *
 * <p>CharFormat is immutable, so sharing one object between many runs is safe: no run
 * can change a format that another run is using.
 *
 * <p>{@link #shared()} is the pool the whole program uses. CharFormat sends every
 * format it creates through {@code FormatPool.shared().intern(...)}, so sharing happens
 * automatically. Tests can make their own pool with {@code new FormatPool()}.
 */

public final class FormatPool {

    private static final FormatPool SHARED = new FormatPool();

    // Key and value are the same object: the stored copy, looked up by equals/hashCode.
    private final Map<CharFormat, CharFormat> formats = new HashMap<>();

    /** @return the pool shared by the whole program */
    public static FormatPool shared() {
        return SHARED;
    }

    /**
     * Returns the stored format equal to f, storing f first if there is none yet.
     *
     * @param f a format; must not be null
     * @return the one shared object equal to f
     */
    public CharFormat intern(CharFormat f) {
        Objects.requireNonNull(f, "format");
        CharFormat stored = formats.get(f);
        if (stored == null) {
            formats.put(f, f);
            stored = f;
        }
        return stored;
    }

    /** @return the number of distinct formats stored (for the 'formats' command) */
    public int size() {
        return formats.size();
    }
}