package edu.arizona.ece373.editor.cli;

import edu.arizona.ece373.editor.app.Action;
import edu.arizona.ece373.editor.app.Editor;
import edu.arizona.ece373.editor.app.Result;
import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import edu.arizona.ece373.editor.history.DeleteTextCommand;
import edu.arizona.ece373.editor.history.FormatTextCommand;
import edu.arizona.ece373.editor.history.FormatTitleCommand;
import edu.arizona.ece373.editor.history.InsertTextCommand;
import edu.arizona.ece373.editor.history.MergeParagraphsCommand;
import edu.arizona.ece373.editor.history.MoveTextCommand;
import edu.arizona.ece373.editor.history.ReplaceTextCommand;
import edu.arizona.ece373.editor.history.SplitParagraphCommand;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRange;
import edu.arizona.ece373.editor.search.MatchStrategy;
import edu.arizona.ece373.editor.search.SubstringMatch;
import edu.arizona.ece373.editor.search.WholeWordMatch;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import edu.arizona.ece373.editor.model.FormatPool;

/**
 * Turns one line of text into an {@link Action}. The only class that knows command syntax
 * (design doc Section 1.3).
 *
 * <p>Factory pattern: a table of {@link ActionFactory} lambdas keyed by verb. Adding a command
 * means adding one {@link #register} call in the constructor. Verbs may be one word
 * ({@code undo}) or two ({@code section add}); a two-word match wins over a one-word one. The
 * parser never touches the Document; the actions it builds call Editor methods when run.
 *
 * <p>Every verb in the Section 1.3 table is parsed and its arguments are checked. Verbs whose
 * engine support has not landed yet build an action that reports "not available yet" when
 * run; each is marked {@code TODO} with the Editor call to make once it exists.
 *
 * <p>Satisfies 6.1.1, 6.1.3, 6.1.5.
 */
public final class CommandParser {

    private static final Set<String> TOGGLE_VALUES = Set.of("on", "off");

    private final Map<String, ActionFactory> factories = new LinkedHashMap<>();
    /** First words of two-word verbs, e.g. {@code section}, for better error messages. */
    private final Set<String> prefixes = new HashSet<>();

    private static final int MAX_SCRIPT_DEPTH = 10;
    /** How many {@code run} commands are in progress, to stop runaway recursion. */
    private int scriptDepth;

    /** Registers every supported verb. */
    public CommandParser() {
        registerStructure();
        registerEditing();
        registerHistory();
        registerFormatting();
        registerViewing();
        registerSearch();
        registerAutomation();
        registerHelp();
    }

    // ---------------------------------------------------------------------------------------
    // Verb table
    // ---------------------------------------------------------------------------------------

    /** UC1: section add [parent] "title" · para add &lt;section&gt; "text" · outline. */
    private void registerStructure() {
        register("section add", a -> {
            SectionPath parent = null;
            String title;
            if (a.count() > 0 && !a.isQuoted(0)) {
                parent = a.sectionPath(0);
                title = a.text(1);
                a.expectAtMost(2);
            } else {
                title = a.text(0);
                a.expectAtMost(1);
            }
            SectionPath p = parent;
            return e -> e.perform(new AddSectionCommand(p, title));
        });

        register("para add", a -> {
            SectionPath section = a.sectionPath(0);
            String text = a.text(1);
            a.expectAtMost(2);
            return e -> e.perform(new AddParagraphCommand(section, text));
        });

        register("outline", a -> {
            a.expectAtMost(0);
            return Editor::outline;
        });
    }

    /** UC1: insert · delete · replace · move · split · merge. */
    private void registerEditing() {
        register("insert", a -> {
            TextRange at = a.position(0);
            String text = a.text(1);
            a.expectAtMost(2);
            return e -> e.perform(new InsertTextCommand(at.paragraph, at.start, text));
        });

        register("delete", a -> {
            TextRange range = a.range(0);
            a.expectAtMost(1);
            return e -> e.perform(new DeleteTextCommand(range));
        });

        register("replace", a -> {
            TextRange range = a.range(0);
            String text = a.text(1);
            a.expectAtMost(2);
            return e -> e.perform(new ReplaceTextCommand(range, text));
        });

        register("move", a -> {
            TextRange source = a.range(0);
            TextRange dest = a.position(1);
            a.expectAtMost(2);
            return e -> e.perform(new MoveTextCommand(source, dest.paragraph, dest.start));
        });

        register("split", a -> {
            TextRange at = a.position(0);
            a.expectAtMost(1);
            return e -> e.perform(new SplitParagraphCommand(at.paragraph, at.start));
        });

        register("merge", a -> {
            int paragraph = a.paragraph(0);
            a.expectAtMost(1);
            return e -> e.perform(new MergeParagraphsCommand(paragraph));
        });
    }

    /** UC2: undo · redo · checkpoint save/restore/list. */
    private void registerHistory() {
        register("undo", a -> {
            a.expectAtMost(0);
            return notRecorded(Editor::undo);
        });

        register("redo", a -> {
            a.expectAtMost(0);
            return notRecorded(Editor::redo);
        });

        register("checkpoint save", a -> {
            String name = a.word(0);
            a.expectAtMost(1);
            return e -> e.saveCheckpoint(name);
        });

        register("checkpoint restore", a -> {
            String name = a.word(0);
            a.expectAtMost(1);
            return e -> e.restoreCheckpoint(name);
        });

        register("checkpoint list", a -> {
            a.expectAtMost(0);
            return Editor::listCheckpoints;
        });
    }

    /** UC3: format &lt;range&gt; &lt;attr&gt; &lt;value&gt; · format title &lt;section&gt; ... · formats. */
    private void registerFormatting() {
        register("format", a -> {
            TextRange range = a.range(0);
            String[] change = formatChange(a, 1);
            // Value ranges are checked in validate, so a bad value is a normal error result.
            return e -> e.perform(new FormatTextCommand(range, change));
        });

        register("format title", a -> {
            SectionPath section = a.sectionPath(0);
            String[] change = formatChange(a, 1);
            return e -> e.perform(new FormatTitleCommand(section, change));
        });

        // Optional requirement 6.5.6 (stretch goal).
        register("formats", a -> {
            a.expectAtMost(0);
            return e -> Result.ok(FormatPool.shared().size() + " distinct formats stored");
        });
    }

    /** UC4: view plain|markup · width n · breaker greedy|balanced · show · stats. */
    private void registerViewing() {
        register("view", a -> {
            String mode = oneOf(a, 0, "view", Set.of("plain", "markup"));
            a.expectAtMost(1);
            return e -> e.setView(mode);
        });

        register("width", a -> {
            int width = a.number(0);
            a.expectAtMost(1);
            // Range 10-200 is checked by ViewSettings so the previous width is kept (UC4 1a).
            return e -> e.setWidth(width);
        });

        register("breaker", a -> {
            String kind = oneOf(a, 0, "breaker", Set.of("greedy", "balanced"));
            a.expectAtMost(1);
            return e -> e.setBreaker(kind);
        });

        register("show", a -> {
            a.expectAtMost(0);
            return Editor::show;
        });

        register("stats", a -> {
            a.expectAtMost(0);
            return e -> Result.ok(e.stats().toString());
        });
    }

    /** UC5: find "q" [word] · replace-first "old" "new" [word] · replace-all "old" "new" [word]. */
    private void registerSearch() {
        register("find", a -> {
            String query = a.text(0);
            boolean word = wordFlag(a, 1);
            return e -> e.find(query, strategy(word));
        });

        register("replace-first", a -> {
            String query = a.text(0);
            String replacement = a.text(1);
            boolean word = wordFlag(a, 2);
            return e -> e.replaceFirst(query, replacement, strategy(word));
        });

        register("replace-all", a -> {
            String query = a.text(0);
            String replacement = a.text(1);
            boolean word = wordFlag(a, 2);
            return e -> e.replaceAll(query, replacement, strategy(word));
        });
    }

    /** UC6: macro record/end/play · run &lt;file&gt; · quit. None of these are recorded (1b). */
    private void registerAutomation() {
        register("macro record", a -> {
            String name = a.word(0);
            a.expectAtMost(1);
            return notRecorded(e -> e.startMacro(name));
        });

        register("macro end", a -> {
            a.expectAtMost(0);
            return notRecorded(Editor::endMacro);
        });

        register("macro play", a -> {
            String name = a.word(0);
            a.expectAtMost(1);
            return notRecorded(e -> e.playMacro(name));
        });

        register("run", a -> {
            String file = a.isQuoted(0) ? a.text(0) : a.word(0);
            a.expectAtMost(1);
            return notRecorded(e -> runScript(file, e));
        });

        register("quit", a -> {
            a.expectAtMost(0);
            return notRecorded(e -> Result.quit("Goodbye"));
        });
    }

    /** help: lists every command with an example. Not recorded in macros. */
    private void registerHelp() {
        register("help", a -> {
            a.expectAtMost(0);
            return notRecorded(e -> Result.ok(HELP));
        });
    }

    private static final String HELP = """
            Positions: pN:i is paragraph N, offset i. Ranges: pN:i-j is characters i up to j.

            Structure
              section add "Title"            add a top-level section
              section add 1 "Title"          add a subsection under section 1
              para add 1 "text"              add a paragraph at the end of section 1
              outline                        show the numbered section outline
            Editing
              insert p1:5 "text"             insert text at paragraph 1, offset 5
              delete p1:0-5                  delete characters 0-5 of paragraph 1
              replace p1:0-5 "text"          replace characters 0-5 with new text
              move p1:0-5 p1:12              move characters 0-5 to offset 12
              split p1:5                     split paragraph 1 into two at offset 5
              merge p1                       join paragraph 2 onto the end of paragraph 1
            History
              undo / redo
              checkpoint save NAME           save the whole document under a name
              checkpoint restore NAME        go back to a saved version (undoable)
              checkpoint list
            Formatting
              format p1:0-5 bold on          also italic on|off, font "Name", size N, color NAME
              format title 1 bold on         format section 1's title
              formats                        how many distinct formats are stored
            Viewing
              show                           print the document
              view plain|markup              plain text, or with formatting markers
              width N                        line width, 10-200
              breaker greedy|balanced        how lines are wrapped
              stats                          word, character and paragraph counts
            Search
              find "text" [word]             list matches; add 'word' for whole words only
              replace-first "old" "new" [word]
              replace-all "old" "new" [word] one undo puts everything back
            Automation
              macro record NAME / macro end / macro play NAME
              run FILE                       run a script file of commands
              help / quit""";

    // ---------------------------------------------------------------------------------------
    // Parsing
    // ---------------------------------------------------------------------------------------

    /**
     * Parses one command line.
     *
     * @param line the text the user typed
     * @return the action, ready to run
     * @throws ParseException with a one-line message if the verb is unknown or an argument is
     *     missing or malformed
     */
    public Action parse(String line) throws ParseException {
        if (line == null || line.isBlank()) {
            throw new ParseException("empty command");
        }
        Tokens all = Tokens.parse(line);
        if (all.isQuoted(0)) {
            throw new ParseException("a command must start with a verb, not quoted text");
        }
        String first = all.word(0);

        if (all.count() >= 2 && !all.isQuoted(1)) {
            ActionFactory two = factories.get(first + " " + all.word(1));
            if (two != null) {
                return two.create(all.from(2));
            }
        }
        ActionFactory one = factories.get(first);
        if (one != null) {
            return one.create(all.from(1));
        }
        if (prefixes.contains(first)) {
            if (all.count() < 2 || all.isQuoted(1)) {
                throw new ParseException("'" + first + "' needs one of: "
                        + String.join(", ", secondWords(first)));
            }
            throw new ParseException("unknown command '" + first + " " + all.word(1)
                    + "'; '" + first + "' takes: " + String.join(", ", secondWords(first)));
        }
        throw new ParseException("unknown command '" + first + "'");
    }

    /** @return every registered verb, in registration order */
    public Set<String> verbs() {
        return Collections.unmodifiableSet(factories.keySet());
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private void register(String verb, ActionFactory factory) {
        factories.put(verb, factory);
        int space = verb.indexOf(' ');
        if (space > 0) {
            prefixes.add(verb.substring(0, space));
        }
    }

    private Set<String> secondWords(String first) {
        Set<String> out = new TreeSet<>();
        for (String verb : factories.keySet()) {
            if (verb.startsWith(first + " ")) {
                out.add(verb.substring(first.length() + 1));
            }
        }
        return out;
    }

    /**
     * Parses {@code <attr> <value>} starting at token {@code i}: {@code bold on|off},
     * {@code italic on|off}, {@code font "Name"}, {@code size n}, {@code color name|#RRGGBB}.
     * Value ranges (size 6-72, known colour names) are checked by FormatChange, not here.
     *
     * @return {attribute, value}
     */
    private static String[] formatChange(Tokens a, int i) throws ParseException {
        String attr = a.word(i);
        String value;
        switch (attr) {
            case "bold", "italic" -> value = oneOf(a, i + 1, attr, TOGGLE_VALUES);
            case "font" -> value = a.text(i + 1);
            case "size" -> value = Integer.toString(a.number(i + 1));
            case "color" -> value = a.word(i + 1);
            default -> throw new ParseException("unknown format attribute '" + attr
                    + "'; use bold, italic, font, size or color");
        }
        a.expectAtMost(i + 2);
        return new String[] {attr, value};
    }

    /** Reads a bare word that must be one of {@code allowed}. */
    private static String oneOf(Tokens a, int i, String what, Set<String> allowed)
            throws ParseException {
        String value = a.word(i);
        if (!allowed.contains(value)) {
            throw new ParseException(what + " must be one of " + new TreeSet<>(allowed)
                    + ", got '" + value + "'");
        }
        return value;
    }

    /**
     * Reads the optional {@code word} flag at token {@code i}, rejecting anything else there
     * and anything after it.
     */
    private static boolean wordFlag(Tokens a, int i) throws ParseException {
        if (a.count() <= i) {
            return false;
        }
        if (a.isQuoted(i) || !a.word(i).equals("word")) {
            throw new ParseException("unexpected argument; the only option here is 'word'");
        }
        a.expectAtMost(i + 1);
        return true;
    }

    /** @return whole-word matching if {@code word}, else substring matching */
    private static MatchStrategy strategy(boolean word) {
        return word ? new WholeWordMatch() : new SubstringMatch();
    }

    /**
     * Runs a script for the {@code run} command. Its echoed lines and results are returned as
     * one message, followed by the summary. A {@code quit} inside the script only ends the
     * script. Scripts may run other scripts, up to {@value #MAX_SCRIPT_DEPTH} deep, so a
     * script that runs itself stops with an error instead of looping forever.
     */
    private Result runScript(String file, Editor e) {
        if (scriptDepth >= MAX_SCRIPT_DEPTH) {
            return Result.error("scripts nested more than " + MAX_SCRIPT_DEPTH + " deep");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        Result r;
        scriptDepth++;
        try {
            r = new ScriptRunner(this, e).run(Path.of(file), out);
        } catch (InvalidPathException ex) {
            return Result.error("bad file name '" + file + "'");
        } finally {
            scriptDepth--;
        }
        if (!r.ok) {
            return r;
        }
        String echoed = buffer.toString(StandardCharsets.UTF_8).stripTrailing();
        String summary = r.quit ? "Script ended with quit" : r.message;
        return Result.ok(echoed.isEmpty() ? summary : echoed + "\n" + summary);
    }

    /** An action for a verb whose engine support has not landed yet. */
    private static Action notYet(String verb) {
        return e -> Result.error("'" + verb + "' is not available yet");
    }

    /** Wraps an action so macros do not record it (UC6 alternate flow 1b). */
    private static Action notRecorded(Action inner) {
        return new Action() {
            @Override
            public Result run(Editor e) {
                return inner.run(e);
            }

            @Override
            public boolean recordable() {
                return false;
            }
        };
    }
}
