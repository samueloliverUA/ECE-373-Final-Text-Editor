/*
 * ConsoleApp.java   (class)
 * Package:   edu.arizona.ece373.editor.cli
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Satisfies: 6.1.1, 6.1.2, 6.1.4
 * Design doc: Fig 2a (cli package), Fig 3
 *
 * PURPOSE: Program entry point. Runs the interactive prompt, or a script passed on the command
 *   line.
 *
 * MEMBERS (from the class diagram):
 *   - +main(args : String[])
 *
 * WHAT TO DO:
 *   1. main(args): build one Editor, one CommandParser and one ScriptRunner. Nothing else
 *      creates these.
 *   2. If args[0] is given, run that script with ScriptRunner and exit; otherwise start the
 *      interactive loop.
 *   3. Interactive loop: print a prompt, read one line, parser.parse(line), action.run(editor),
 *      print result.message. 'quit' ends the loop.
 *   4. Catch ParseException and print 'Error: <message>'. The program must never crash or print
 *      a stack trace on bad input.
 *   5. Write all output through a PrintStream passed in (not System.out directly inside logic)
 *      so ScenarioTest can capture it.
 *
 * TESTED BY: ScenarioTest (end to end), manual run
 *
 * DONE WHEN: java -jar target/editor.jar runs an interactive session and java -jar
 *   target/editor.jar demo.txt runs a script.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
