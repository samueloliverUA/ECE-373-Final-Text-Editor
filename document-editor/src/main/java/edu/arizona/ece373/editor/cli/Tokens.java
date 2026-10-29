/*
 * Tokens.java   (class)
 * Package:   edu.arizona.ece373.editor.cli
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Oct 28
 * Design doc: Fig 2a (parameter type of ActionFactory.create)
 *
 * PURPOSE: The parsed arguments of one command line, with typed accessors that throw
 *   ParseException on bad input.
 *
 * WHAT TO DO:
 *   1. Hold the token list after the verb.
 *   2. Accessors: text(i) for quoted strings, number(i), position(i) (p2:14), range(i) (p2:5-12
 *      -> TextRange), sectionPath(i) (2.1 -> SectionPath), hasFlag("word"), count().
 *   3. Each accessor throws ParseException naming the argument, e.g. 'expected a range like
 *      p2:5-12'.
 *
 * TESTED BY: CommandParserTest
 *
 * NOTE: Tokens appears only as a parameter type in the class diagram. Add it as a box to Figure
 *   2a when you next update the diagram.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
