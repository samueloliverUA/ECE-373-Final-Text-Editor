/*
 * ScriptRunner.java   (class)
 * Package:   edu.arizona.ece373.editor.cli
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 16
 * Satisfies: 6.1.2, 6.9.7 (same input -> same output)
 * Design doc: Fig 2a; UC6 alternate flows 4a-4c
 *
 * PURPOSE: Runs a text file of commands line by line, exactly as if each line were typed.
 *
 * MEMBERS (from the class diagram):
 *   - +run(path : Path, out : PrintStream)
 *
 * WHAT TO DO:
 *   1. run(path, out): read the file one line at a time.
 *   2. Skip blank lines and lines starting with '#'.
 *   3. Echo each command ('> insert p2:14 "x"') then parse and run it through the same
 *      CommandParser.
 *   4. If a line fails, print 'line N: Error: <message>' and CONTINUE with the next line.
 *   5. If the file does not exist, return an error result. Also used by the 'run <file>'
 *      command.
 *
 * TESTED BY: ScriptRunnerTest, ScenarioTest
 *
 * DONE WHEN: Running the same script twice produces byte-for-byte identical output.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
