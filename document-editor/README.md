# Document Editor — ECE 373 Final Project

Team: Jack Erdman and Sam Oliver

A Java 17 console document editor built to the System ConOps and Requirements. The design is in
`docs/Design_Document.pdf`; this repository follows its class diagram (Figures 2a and 2b) file for file.

**Status: skeleton only.** Every `.java` file is a placeholder whose header comment names the owner, reviewer,
week, requirements, and exactly what to build. Replace the comment with the class when you start the file.
The full checklist is in [`docs/TASKS.md`](docs/TASKS.md).

## Build and run

```
mvn clean test                          # must pass from a fresh clone at all times
mvn clean package
java -jar target/editor.jar             # interactive
java -jar target/editor.jar demo.txt    # run a script
```

## Layout

| Package | Layer | Owner | What lives there |
|---|---|---|---|
| `cli` | Command Layer | Sam | Console loop, command parser, script runner. The only code that knows command syntax. |
| `app` | Engine entry point | Shared | `Editor` facade (shared), `Action`, `MacroLibrary` (Sam), `Result`. |
| `model` | Document Engine | Sam (Jack: listener, event, memento, FormatPool) | Document tree (Composite), formatted text, formats. |
| `history` | Document Engine | Jack (Sam: the two format commands) | `EditCommand`s, `History` (undo/redo), checkpoints. |
| `search` | Document Engine | Jack | Find, replace-first, Replace All, match strategies. |
| `view` | Presentation Layer | Sam (Jack: statistics) | Renderers, line breakers, view settings, statistics. |

Tests mirror the packages under `src/test/java`. `ScenarioTest` runs `src/test/resources/scenario.txt`.

## Working rules (design document, Section 7.2)

1. `main` is protected. Every change goes through a pull request reviewed by the person who did not write it.
2. Small commits, each of which builds. Name branches after the feature, e.g. `feature/undo-redo`.
3. CI (`.github/workflows/build.yml`) runs `mvn clean test` on every pull request; a red build is not merged.
4. Interfaces first: in week 1, merge the interfaces (`DocumentNode`, `FormattedText`, `EditCommand`, `History`,
   `Renderer`, `LineBreaker`, `DocumentListener`, `Action`) so each person codes against agreed signatures.
5. JavaDoc on every public class and method before feature freeze (week of Nov 23).
6. `Editor.java` is shared: each person edits only their own methods, in their own pull request.
