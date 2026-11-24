# Document Editor

**ECE 373 Final Team Project** by Jack Erdman and Sam Oliver

A Java 17 console editor for structured documents. Authors build a document from nested sections and paragraphs, then edit and format its text, undo any change, save named checkpoints, search and replace, view it with different line-wrapping strategies, and automate work with macros and scripts.

Documents exist only for the current session. The editor has no command for saving a document to a file.

## Getting started

You need Java 17 or later and Maven 3.9 or later. Maven downloads JUnit 5 the first time it runs. The project has no other dependencies.

The Maven project is in the `document-editor` folder, so run these commands from there:

```bash
cd document-editor

# Compile and run the full test suite
mvn clean test

# Build the executable jar
mvn clean package

# Start an interactive session
java -jar target/editor.jar

# Run a file of commands
java -jar target/editor.jar src/test/resources/scenario.txt
```

## Example session

```text
> section add "Introduction"
Added section 1 "Introduction"
> para add 1 "The cat sat on the mat."
Added paragraph 1 to section 1
> format p1:4-7 bold on
Formatted p1:4-7 bold on
> view markup
View: markup
> show
1 Introduction

The *cat* sat on the mat.
> replace-all "cat" "dog"
Replace all with "dog" (1)
> stats
Words: 6, characters: 23, paragraphs: 1
> undo
Undid: replace all with "dog" (1)
> quit
Goodbye
```

## Commands

Paragraphs are numbered across the whole document. `p2:14` is offset 14 in paragraph 2, and `p2:5-12` is characters 5 up to, but not including, 12. Type `help` in the editor for a summary.

| Area | Commands |
|---|---|
| Structure | `section add [parent] "title"`, `para add <section> "text"`, `outline` |
| Editing | `insert p1:5 "text"`, `delete p1:0-5`, `replace p1:0-5 "text"`, `move p1:0-5 p1:12`, `split p1:5`, `merge p1` |
| History | `undo`, `redo`, `checkpoint save <name>`, `checkpoint restore <name>`, `checkpoint list` |
| Formatting | `format p1:0-5 bold on`, `italic on`, `font "Arial"`, `size 14`, `color red`, `format title 1 size 18`, `formats` |
| Viewing | `show`, `view plain`, `view markup`, `width 50`, `breaker greedy`, `breaker balanced`, `stats` |
| Search | `find "text" [word]`, `replace-first "old" "new" [word]`, `replace-all "old" "new" [word]` |
| Automation | `macro record <name>`, `macro end`, `macro play <name>`, `run <file>` |
| Session | `help`, `quit` |

Add `word` to a search command to match whole words only. A failed command prints an error and leaves the document unchanged.

## Use cases

| Use case | What the user can do |
|---|---|
| UC1 | Build and edit the document |
| UC2 | Undo, redo, and restore checkpoints |
| UC3 | Format text |
| UC4 | View the document and its statistics |
| UC5 | Find and replace text |
| UC6 | Automate with macros and scripts |

`ScenarioTest` runs all six use cases from [scenario.txt](document-editor/src/test/resources/scenario.txt) and compares the output with [scenario-expected.txt](document-editor/src/test/resources/scenario-expected.txt).

## Architecture

Source code is under `document-editor/src/main/java/edu/arizona/ece373/editor/`.

| Package | Layer | Responsibility |
|---|---|---|
| `cli` | Command | Console loop, command parser, and script runner. This is the only package that knows command syntax. |
| `app` | Engine entry point | The `Editor` facade, actions, results, and macros. |
| `model` | Document engine | The document tree, formatted text, formats, change events, and mementos. |
| `history` | Document engine | Undoable edit commands, undo and redo stacks, and checkpoints. |
| `search` | Document engine | Find, replace, and match strategies. |
| `view` | Presentation | Renderers, line breakers, view settings, and statistics. |

### Design patterns

| Pattern | GoF family | Where it is used |
|---|---|---|
| Command | Behavioral | `EditCommand` and its implementations make every edit undoable. |
| Memento | Behavioral | `DocumentMemento` and `CheckpointStore` save and restore the document. |
| Observer | Behavioral | `DocumentListener` lets `DocumentStatistics` react to document changes. |
| Strategy | Behavioral | `MatchStrategy` selects search matching, and `LineBreaker` selects line wrapping. |
| Template Method | Behavioral | `AbstractRenderer` defines rendering, and subclasses supply the tokens. |
| Composite | Structural | `Section` and `Paragraph` share `DocumentNode`, and `CompositeCommand` groups edits. |
| Facade | Structural | `Editor` is the single entry point into the engine. |
| Flyweight | Structural | `FormatPool` shares identical `CharFormat` objects. |

## Testing

The JUnit 5 suite has 906 tests, organized to mirror the main packages under `document-editor/src/test/java`. It covers individual classes, undo and redo, error handling, deep nesting, a large document, and the full end-to-end scenario.

## Project documents

- [Design document](document-editor/docs/Design_Document.pdf)
- [Task list](document-editor/docs/TASKS.md)
- [Project proposal and earlier design files](proposal/)

## How I Worked with an AI Agent

We used Claude Code (Claude Opus 5.5) in VS Code, this was the main use of AI for our final
project.

Claude Code predominantly assisted with writing repetitive lines of code such as the test files and extensive uses cases. Most of the logic behind the code was predetermined by us 
(Sam and Jack) and then was created using AI tasked with following our strict outline for the code logic.

When working with the AI agent we carefully set up a code outline prior to starting in order to keep the AI from creating erroneous code that would not work with the rest of the program. We also outlined the exact logic that each function should use in order to keep the code from getting too large or even unessescary. From there the AI was prompted to complete other funcitons in the form outlined by us (the authors) and stay away from interfering with other files or functions. 

One example of how we worked with AI is the `MoveTextCommandTest` file, in this file we are testing the ability to move text to another spot in the same paragraph. This required us to create various tests that check all the possible places the text can be moved as well as undo and redo funcitons and how this changes where the text will be at. This file required us to review various times since we had to find out that the text range that was being moved was actually acurate. This was also one of the more repetitive files since we had to test all the edge cases as well and out of boundary cases. What we did as the authors was verify the text lengths as well as where the text should be placed, something that took a lot of time to read through but we were able to catch small indexing errors. 