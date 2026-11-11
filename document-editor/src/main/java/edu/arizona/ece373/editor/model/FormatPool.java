/*
 * FormatPool.java   (class)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 30 (stretch)
 * Pattern:   Flyweight (factory)
 * Satisfies: 6.5.6 (optional)
 * Design doc: Fig 2b; Sections 1.2, 5.6
 *
 * PURPOSE: STRETCH GOAL ONLY (optional requirement 6.5.6): shares identical CharFormat objects.
 *
 * MEMBERS (from the class diagram):
 *   - +intern(f : CharFormat) : CharFormat
 *   - +size() : int
 *
 * WHAT TO DO:
 *   1. Start only after every core use case passes its tests.
 *   2. intern(f): return the already-stored equal CharFormat, or store and return f.
 *   3. size(): number of distinct formats stored (for the 'formats' command).
 *   4. Route every CharFormat creation through intern() so sharing is automatic.
 *
 * TESTED BY: FormatPoolTest
 *
 * NOTE: Work plan gives this to Jack in the Nov 30 week. If it is not reached, delete this file
 *   and FormatPoolTest so the design matches the code.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
