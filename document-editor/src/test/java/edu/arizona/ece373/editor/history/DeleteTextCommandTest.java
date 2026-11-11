/*
 * DeleteTextCommandTest.java   (JUnit 5 test class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 2
 * Satisfies: 6.3.2
 *
 * PURPOSE: Tests DeleteTextCommand.
 *
 * WHAT TO DO:
 *   1. Main case works.
 *   2. Every invalid input is rejected by validate and the document is unchanged.
 *   3. Boundary positions: offset 0, offset = length, empty paragraph.
 *   4. Undo round trip: execute then undo gives exactly the original (text AND formatting).
 *
 * DONE WHEN: All cases pass under mvn clean test, including the boundary cases.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
