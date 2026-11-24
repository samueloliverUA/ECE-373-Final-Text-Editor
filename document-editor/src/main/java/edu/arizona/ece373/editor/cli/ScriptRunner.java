package edu.arizona.ece373.editor.cli;

import edu.arizona.ece373.editor.app.Editor;
import edu.arizona.ece373.editor.app.Result;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs a text file of commands line by line, exactly as if each line were typed (UC6 step 4).
 *
 * <p>Blank lines and lines starting with {@code #} are skipped. Each command is echoed as
 * {@code > command}, then parsed and run through the same {@link CommandParser} as the
 * console. A failing line prints {@code line N: Error: message} and the script continues
 * (UC6 4b), so the same script always gives the same output (6.9.7). {@code quit} ends the
 * script.
 *
 * <p>Satisfies 6.1.2, 6.9.7.
 */
public final class ScriptRunner {

    private final CommandParser parser;
    private final Editor editor;

    /**
     * Creates a script runner.
     *
     * @param parser the parser shared with the console
     * @param editor the editor every command runs against
     */
    public ScriptRunner(CommandParser parser, Editor editor) {
        this.parser = parser;
        this.editor = editor;
    }

    /**
     * Runs every command in a script file.
     *
     * @param path the script file
     * @param out where to write the echoed commands and results
     * @return ok when the file was read to the end (even if some lines failed); an error if
     *     the file could not be read; a quit result if the script ran {@code quit}
     */
    public Result run(Path path, PrintStream out) {
        if (!Files.isRegularFile(path)) {
            return Result.error("No such script file: " + path);
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            int failures = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String command = line.strip();
                if (command.isEmpty() || command.startsWith("#")) {
                    continue;
                }
                out.print("> " + command + "\n");
                Result result = runLine(command);
                if (!result.ok) {
                    failures++;
                    out.print("line " + lineNumber + ": Error: " + result.message + "\n");
                } else {
                    out.print(result.message + "\n");
                }
                if (result.quit) {
                    return result;
                }
            }
            return Result.ok("Ran " + path.getFileName() + " (" + lineNumber + " lines, "
                    + failures + (failures == 1 ? " error)" : " errors)"));
        } catch (IOException e) {
            return Result.error("Could not read " + path + ": " + e.getMessage());
        }
    }

    /** Parses and runs one line; never throws. */
    private Result runLine(String command) {
        try {
            return editor.run(parser.parse(command)); // recorded like a typed command
        } catch (ParseException e) {
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            return Result.error("internal error: " + e);
        }
    }
}
