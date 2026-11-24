package edu.arizona.ece373.editor.cli;

import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRange;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The tokens of a command line, with typed accessors that throw {@link ParseException} on
 * bad input. Tokens are separated by whitespace; a token starting with {@code "} runs to the
 * matching closing quote and may contain spaces ({@code \"} and {@code \\} escape a quote and
 * a backslash inside it).
 *
 * <p>Syntax (design doc Section 1.3): positions {@code p2:14}, ranges {@code p2:5-12} (end
 * excluded), section paths {@code 2.1}, quoted text {@code "two words"}.
 */
public final class Tokens {

    private static final Pattern POSITION = Pattern.compile("p(\\d+):(\\d+)");
    private static final Pattern RANGE = Pattern.compile("p(\\d+):(\\d+)-(\\d+)");
    private static final Pattern PARAGRAPH = Pattern.compile("p(\\d+)");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+");

    /** One token and whether it was written in quotes. */
    private record Token(String value, boolean quoted) {
        String shown() {
            return quoted ? "\"" + value + "\"" : value;
        }
    }

    private final List<Token> tokens;

    private Tokens(List<Token> tokens) {
        this.tokens = Collections.unmodifiableList(tokens);
    }

    /**
     * Splits text into tokens.
     *
     * @param text the text to split (a whole line, or the arguments after the verb)
     * @return the tokens
     * @throws ParseException if a quote is not closed
     */
    public static Tokens parse(String text) throws ParseException {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                boolean closed = false;
                while (i < n) {
                    char q = text.charAt(i);
                    if (q == '\\' && i + 1 < n
                            && (text.charAt(i + 1) == '"' || text.charAt(i + 1) == '\\')) {
                        sb.append(text.charAt(i + 1));
                        i += 2;
                    } else if (q == '"') {
                        closed = true;
                        i++;
                        break;
                    } else {
                        sb.append(q);
                        i++;
                    }
                }
                if (!closed) {
                    throw new ParseException("missing closing quote in: " + text.strip());
                }
                out.add(new Token(sb.toString(), true));
            } else {
                int start = i;
                while (i < n && !Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                out.add(new Token(text.substring(start, i), false));
            }
        }
        return new Tokens(out);
    }

    /**
     * Returns the tokens from an index onward, used to drop the verb.
     *
     * @param from index of the first token to keep
     * @return the tokens from {@code from} on (used to drop the verb)
     */
    public Tokens from(int from) {
        return new Tokens(new ArrayList<>(tokens.subList(Math.min(from, tokens.size()),
                tokens.size())));
    }

    /**
     * Returns the number of tokens.
     *
     * @return the number of tokens
     */
    public int count() {
        return tokens.size();
    }

    /**
     * Checks whether token {@code i} exists and was written in quotes.
     *
     * @param i token index
     * @return true if token {@code i} exists and was written in quotes
     */
    public boolean isQuoted(int i) {
        return i < tokens.size() && tokens.get(i).quoted();
    }

    /**
     * Checks whether an unquoted token equals {@code word}.
     *
     * @param word a flag such as {@code word}
     * @return true if an unquoted token equals {@code word}
     */
    public boolean hasFlag(String word) {
        for (Token t : tokens) {
            if (!t.quoted() && t.value().equals(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads a token as quoted text.
     *
     * @param i token index
     * @return the contents of a quoted string
     * @throws ParseException if missing or not quoted
     */
    public String text(int i) throws ParseException {
        Token t = get(i, "quoted text like \"Introduction\"");
        if (!t.quoted()) {
            throw new ParseException("expected quoted text like \"Introduction\", got '"
                    + t.value() + "'");
        }
        return t.value();
    }

    /**
     * Reads a token as a bare word.
     *
     * @param i token index
     * @return a bare (unquoted) word, such as a checkpoint name or {@code plain}
     * @throws ParseException if missing or quoted
     */
    public String word(int i) throws ParseException {
        Token t = get(i, "a name");
        if (t.quoted()) {
            throw new ParseException("expected a name without quotes, got " + t.shown());
        }
        return t.value();
    }

    /**
     * Reads a token as a whole number.
     *
     * @param i token index
     * @return the token as an integer
     * @throws ParseException if missing or not a whole number
     */
    public int number(int i) throws ParseException {
        Token t = get(i, "a number");
        if (t.quoted() || !NUMBER.matcher(t.value()).matches()) {
            throw new ParseException("expected a number, got " + t.shown());
        }
        try {
            return Integer.parseInt(t.value());
        } catch (NumberFormatException e) {
            throw new ParseException("number too large: " + t.value());
        }
    }

    /**
     * Reads a token as a position.
     *
     * @param i token index
     * @return a position such as {@code p2:14}, as a range with {@code start == end}
     * @throws ParseException if missing or malformed
     */
    public TextRange position(int i) throws ParseException {
        String what = "a position like p2:14";
        Token t = get(i, what);
        Matcher m = match(POSITION, t, what);
        int p = paragraphNumber(m.group(1), t);
        int off = toInt(m.group(2), t);
        return new TextRange(p, off, off);
    }

    /**
     * Reads a token as a character range.
     *
     * @param i token index
     * @return a range such as {@code p2:5-12}
     * @throws ParseException if missing, malformed, or the start is after the end
     */
    public TextRange range(int i) throws ParseException {
        String what = "a range like p2:5-12";
        Token t = get(i, what);
        Matcher m = match(RANGE, t, what);
        int p = paragraphNumber(m.group(1), t);
        int start = toInt(m.group(2), t);
        int end = toInt(m.group(3), t);
        if (start > end) {
            throw new ParseException("start is after end in '" + t.value() + "'");
        }
        return new TextRange(p, start, end);
    }

    /**
     * Reads a token as a paragraph number.
     *
     * @param i token index
     * @return a paragraph number written {@code p2}
     * @throws ParseException if missing or malformed
     */
    public int paragraph(int i) throws ParseException {
        String what = "a paragraph like p2";
        Token t = get(i, what);
        Matcher m = match(PARAGRAPH, t, what);
        return paragraphNumber(m.group(1), t);
    }

    /**
     * Reads a token as a section path.
     *
     * @param i token index
     * @return a section path such as {@code 2.1}
     * @throws ParseException if missing or malformed
     */
    public SectionPath sectionPath(int i) throws ParseException {
        Token t = get(i, "a section path like 2.1");
        if (t.quoted()) {
            throw new ParseException("expected a section path like 2.1, got " + t.shown());
        }
        try {
            return SectionPath.parse(t.value());
        } catch (IllegalArgumentException e) {
            throw new ParseException("expected a section path like 2.1, got '"
                    + t.value() + "'");
        }
    }

    /**
     * Rejects extra arguments.
     *
     * @param max the most tokens this command accepts
     * @throws ParseException naming the first extra token
     */
    public void expectAtMost(int max) throws ParseException {
        if (tokens.size() > max) {
            throw new ParseException("unexpected extra argument " + tokens.get(max).shown());
        }
    }

    // --- helpers ---------------------------------------------------------------------------

    private Token get(int i, String what) throws ParseException {
        if (i >= tokens.size()) {
            throw new ParseException("missing argument " + (i + 1) + ": expected " + what);
        }
        return tokens.get(i);
    }

    private static Matcher match(Pattern p, Token t, String what) throws ParseException {
        Matcher m = p.matcher(t.value());
        if (t.quoted() || !m.matches()) {
            throw new ParseException("expected " + what + ", got '" + t.value() + "'");
        }
        return m;
    }

    private static int paragraphNumber(String digits, Token t) throws ParseException {
        int p = toInt(digits, t);
        if (p < 1) {
            throw new ParseException("paragraph numbers start at 1 in '" + t.value() + "'");
        }
        return p;
    }

    private static int toInt(String digits, Token t) throws ParseException {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new ParseException("number too large in '" + t.value() + "'");
        }
    }
}
