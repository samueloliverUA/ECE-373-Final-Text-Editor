# Task list: every file, who owns it, and when

Each file in this project is a placeholder whose header comment says exactly what to build. Tick a box when that file's pull request is merged with its tests passing.

Weeks follow the schedule in Section 7.3 of the design document. Reviewer is always the other person.

| Owner | Files |
|---|---|
| Sam | 46 |
| Jack | 45 |
| Shared | 7 |

## Sam

### Week of Oct 28

- [ ] `main/app/Action.java` — One parsed command, ready to run against the Editor
- [ ] `main/cli/ActionFactory.java` — Creates the Action for one verb from its parsed arguments
- [ ] `main/cli/CommandParser.java` — Turns one line of text into an Action
- [ ] `main/cli/ConsoleApp.java` — Program entry point
- [ ] `main/cli/ParseException.java` — Thrown by CommandParser and Tokens when a command line is not valid syntax
- [ ] `main/cli/Tokens.java` — The parsed arguments of one command line, with typed accessors that throw ParseException on bad input
- [ ] `main/model/Document.java` — The single document: owns the untitled root Section, finds paragraphs and sections, and notifies listeners
- [ ] `main/model/DocumentNode.java` — Common type for Section and Paragraph so every operation works at any nesting depth
- [ ] `main/model/EditException.java` — Thrown by EditCommand.validate (and model lookups) when a change cannot be made
- [ ] `main/model/FormattedText.java` — A string where different characters carry different formatting, stored as a list of runs
- [ ] `main/model/Paragraph.java` — A leaf node holding one block of formatted text
- [ ] `main/model/Section.java` — A titled container of paragraphs and subsections, to any depth
- [ ] `main/model/SectionPath.java` — A section address such as 2.1 (second top-level section, its first subsection)
- [ ] `main/model/TextRange.java` — A character range inside one paragraph: paragraph number, start, end (end exclusive)
- [ ] `main/model/TextRun.java` — A piece of text that has one formatting value throughout
- [ ] `test/cli/CommandParserTest.java` — Tests CommandParser and Tokens
- [ ] `test/model/DocumentTest.java` — Tests Document and Paragraph
- [ ] `test/model/FormattedTextTest.java` — Tests FormattedText and TextRun
- [ ] `test/model/SectionTest.java` — Tests Section and SectionPath

### Week of Nov 2

- [ ] `main/history/FormatTextCommand.java` — Applies one formatting change to a range of characters
- [ ] `main/history/FormatTitleCommand.java` — Applies one formatting change to a whole section title
- [ ] `main/model/CharFormat.java` — The formatting of a character: bold, italic, font, size, color
- [ ] `main/model/FormatChange.java` — Describes a change to exactly one formatting attribute (e.g
- [ ] `test/history/FormatTextCommandTest.java` — Tests FormatTextCommand
- [ ] `test/history/FormatTitleCommandTest.java` — Tests FormatTitleCommand
- [ ] `test/model/CharFormatTest.java` — Tests CharFormat and FormatChange

### Week of Nov 9

- [ ] `main/model/DocumentWriter.java` — Callback the document tree writes itself into; renderers implement it
- [ ] `main/view/AbstractRenderer.java` — Shared rendering logic: walks the tree as a DocumentWriter, numbers headings, wraps paragraphs
- [ ] `main/view/GreedyLineBreaker.java` — Fills each line with as many words as fit
- [ ] `main/view/LineBreaker.java` — Splits a list of words into lines no wider than the width
- [ ] `main/view/MarkupRenderer.java` — Marked-up view: words wrapped in markers that show formatting
- [ ] `main/view/PlainTextRenderer.java` — Plain view: words only, no formatting markers
- [ ] `main/view/Renderer.java` — Turns the whole document into output text
- [ ] `main/view/ViewSettings.java` — Current view (plain or markup), line breaker (greedy or balanced) and width
- [ ] `test/model/DeepNestingTest.java` — Tests operations on deeply nested content
- [ ] `test/view/GreedyLineBreakerTest.java` — Tests GreedyLineBreaker
- [ ] `test/view/MarkupRendererTest.java` — Tests MarkupRenderer
- [ ] `test/view/PlainTextRendererTest.java` — Tests PlainTextRenderer and AbstractRenderer
- [ ] `test/view/ViewSettingsTest.java` — Tests ViewSettings

### Week of Nov 16

- [ ] `main/app/MacroLibrary.java` — Records Actions under a name and replays them as one undoable history entry
- [ ] `main/cli/ScriptRunner.java` — Runs a text file of commands line by line, exactly as if each line were typed
- [ ] `main/view/BalancedLineBreaker.java` — Minimum-raggedness breaking: makes line lengths more even than greedy
- [ ] `test/app/MacroLibraryTest.java` — Tests MacroLibrary
- [ ] `test/cli/ScriptRunnerTest.java` — Tests ScriptRunner
- [ ] `test/view/BalancedLineBreakerTest.java` — Tests BalancedLineBreaker

### Week of Nov 30 (stretch)

- [ ] `scripts/demo.txt` — Live demo script, labelled by use case

## Jack

### Week of Oct 28

- [ ] `.github/workflows/build.yml` — CI: run mvn clean test on every pull request
- [ ] `.gitignore` — Ignore build output and IDE files
- [ ] `pom.xml` — Maven build (Java 17, JUnit 5, editor.jar)
- [ ] `main/history/AddParagraphCommand.java` — Adds a paragraph with text to the end of a section
- [ ] `main/history/AddSectionCommand.java` — Adds a section (top-level or under a parent path) with a title
- [ ] `main/history/CompositeCommand.java` — A list of EditCommands that acts as one: Replace All and macro replays
- [ ] `main/history/EditCommand.java` — One undoable change to the document
- [ ] `main/history/History.java` — Undo and redo stacks with no small limit, plus grouping for macros
- [ ] `test/history/AddParagraphCommandTest.java` — Tests AddParagraphCommand
- [ ] `test/history/AddSectionCommandTest.java` — Tests AddSectionCommand
- [ ] `test/history/HistoryTest.java` — Tests History and CompositeCommand

### Week of Nov 2

- [ ] `main/history/DeleteTextCommand.java` — Deletes a range of text, remembering it (with formatting) for undo
- [ ] `main/history/InsertTextCommand.java` — Inserts text at a position in a paragraph
- [ ] `main/history/MergeParagraphsCommand.java` — Appends the following paragraph to paragraph n and removes it
- [ ] `main/history/MoveTextCommand.java` — Moves a range to another position in the same paragraph
- [ ] `main/history/ReplaceTextCommand.java` — Replaces a range with new text
- [ ] `main/history/SplitParagraphCommand.java` — Splits a paragraph at an offset into two paragraphs in the same section
- [ ] `test/history/DeleteTextCommandTest.java` — Tests DeleteTextCommand
- [ ] `test/history/InsertTextCommandTest.java` — Tests InsertTextCommand
- [ ] `test/history/MergeParagraphsCommandTest.java` — Tests MergeParagraphsCommand
- [ ] `test/history/MoveTextCommandTest.java` — Tests MoveTextCommand
- [ ] `test/history/ReplaceTextCommandTest.java` — Tests ReplaceTextCommand
- [ ] `test/history/SplitParagraphCommandTest.java` — Tests SplitParagraphCommand

### Week of Nov 9

- [ ] `main/history/CheckpointStore.java` — Named checkpoints: name -> DocumentMemento
- [ ] `main/history/RestoreCheckpointCommand.java` — Replaces the document with a saved checkpoint, as an undoable change
- [ ] `main/model/DocumentEvent.java` — What happened in one change, passed to every DocumentListener
- [ ] `main/model/DocumentListener.java` — Anything that wants to hear about document changes (statistics now; auto-save in a future release)
- [ ] `main/model/DocumentMemento.java` — An opaque saved copy of the whole document, used for checkpoints
- [ ] `main/view/DocumentStatistics.java` — Word, character and paragraph counts that stay current without editing commands computing them
- [ ] `main/view/Statistics.java` — Immutable snapshot of the three counts, returned by Editor.stats()
- [ ] `test/history/CheckpointStoreTest.java` — Tests CheckpointStore and DocumentMemento
- [ ] `test/history/RestoreCheckpointCommandTest.java` — Tests RestoreCheckpointCommand
- [ ] `test/view/DocumentStatisticsTest.java` — Tests DocumentStatistics and Statistics

### Week of Nov 16

- [ ] `main/search/Match.java` — One search result: a TextRange
- [ ] `main/search/MatchStrategy.java` — How a query matches inside one string: substring or whole word
- [ ] `main/search/SearchService.java` — Finds matches across the whole document and builds the replace commands
- [ ] `main/search/SubstringMatch.java` — Matches the query anywhere, case-sensitive
- [ ] `main/search/WholeWordMatch.java` — Matches the query only when it is a whole word
- [ ] `test/search/ReplaceAllTest.java` — Tests Replace All
- [ ] `test/search/SearchServiceTest.java` — Tests SearchService find and replace-first
- [ ] `test/search/SubstringMatchTest.java` — Tests SubstringMatch
- [ ] `test/search/WholeWordMatchTest.java` — Tests WholeWordMatch

### Week of Nov 23

- [ ] `test/search/LargeDocumentTest.java` — Tests performance on a 100,000-character document

### Week of Nov 30 (stretch)

- [ ] `main/model/FormatPool.java` — STRETCH GOAL ONLY (optional requirement 6.5.6): shares identical CharFormat objects
- [ ] `test/model/FormatPoolTest.java` — Tests FormatPool (stretch)

## Shared

### Week of Oct 28

- [ ] `main/app/Editor.java` — The single entry point into the engine and the composition root: it creates and wires every engine and view object
- [ ] `main/app/Result.java` — Immutable outcome of a command: success flag plus the one-line message shown to the user
- [ ] `test/app/EditorTest.java` — Tests the Editor facade
- [ ] `test/app/ScenarioTest.java` — Tests the ConOps Section 2.1 scenario end to end
- [ ] `src/test/resources/scenario.txt` — ConOps 2.1 scenario as a command script

### Week of Nov 2

- [ ] `test/app/EditorErrorTest.java` — Tests error handling end to end

### Week of Nov 16

- [ ] `src/test/resources/scenario-expected.txt` — Expected output for ScenarioTest

## Not tied to one file

- [ ] **Sam, week of Nov 23:** JavaDoc on every public class and method (rubric: Testing, Documentation & Version Control).
- [ ] **Jack, week of Nov 23:** boundary-case pass over every test class.
- [ ] **Both, week of Nov 23:** feature freeze; update the design document if the code drifted (class diagram boxes for `Tokens` and `Statistics`, any decisions noted in JavaDoc).
- [ ] **Sam, week of Nov 30:** slides; upload before the deadline in a format that opens on the room machine.
- [ ] **Both, week of Nov 30:** rehearse the design walkthrough and Q&A; each person must be able to explain the other's subsystem.
- [ ] **Each of you, presentation day:** Team Evaluation #2 and the individual reflection.
