package edu.arizona.ece373.editor.app;

import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.model.EditException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Records {@link Action}s under a name and replays them as one undoable history entry
 * (UC6).
 *
 * <p>Recording: {@link #startRecording}, then every action that succeeds and is
 * {@link Action#recordable()} is passed to {@link #record} (by {@link Editor#run}), then
 * {@link #stopRecording} saves the list, overwriting any macro with that name. Undo, redo,
 * macro commands, {@code run} and {@code quit} are never recorded (UC6 1b).
 *
 * <p>Playing: the steps run in order inside one history group, so a single undo reverses the
 * whole replay and redo re-applies it. If a step fails, the group is aborted, which rolls the
 * document back to how it was before the replay, and the failing step is reported (UC6 3b).
 * Steps that are not edits (view, width, checkpoint save, ...) are not part of the history
 * group, so they take effect but are not undone with the replay.
 *
 * <p>Satisfies 6.8.5, 6.8.6, 6.8.7. Design doc Fig 2a, Fig 7; Sections 5.3, 5.8.
 */
public final class MacroLibrary {

    private final Map<String, List<Action>> macros = new TreeMap<>();
    private List<Action> recording;
    private String recordingName;
    private boolean playing;

    /**
     * Starts recording a new macro.
     *
     * @param name the macro name
     * @throws EditException if already recording, or the name is blank
     */
    public void startRecording(String name) {
        if (recording != null) {
            throw new EditException("Already recording macro \"" + recordingName
                    + "\"; use 'macro end' first");
        }
        if (name == null || name.isBlank()) {
            throw new EditException("Macro name cannot be empty");
        }
        recording = new ArrayList<>();
        recordingName = name;
    }

    /**
     * Adds a step to the macro being recorded. Does nothing when not recording, or for an
     * action that is not {@link Action#recordable()}.
     *
     * @param a an action that has just succeeded
     */
    public void record(Action a) {
        if (recording != null && a.recordable()) {
            recording.add(a);
        }
    }

    /**
     * Stops recording and saves the macro, overwriting one with the same name.
     *
     * @return the name the macro was saved under
     * @throws EditException if not recording
     */
    public String stopRecording() {
        if (recording == null) {
            throw new EditException("Not recording a macro; use 'macro record <name>' first");
        }
        String name = recordingName;
        macros.put(name, Collections.unmodifiableList(recording));
        recording = null;
        recordingName = null;
        return name;
    }

    /**
     * Replays a macro as one history entry.
     *
     * @param name the macro to play
     * @param e the editor the steps run against
     * @return success naming the macro and step count, or an error naming the failing step
     *     (the document is then as it was before the replay)
     */
    public Result play(String name, Editor e) {
        List<Action> steps = macros.get(name);
        if (steps == null) {
            return Result.error(macros.isEmpty()
                    ? "No macro \"" + name + "\"; no macros recorded"
                    : "No macro \"" + name + "\"; recorded: " + String.join(", ", names()));
        }
        if (playing) {
            return Result.error("Cannot play a macro while another is playing");
        }
        History history = e.history();
        if (history.isGroupOpen()) {
            return Result.error("Cannot play a macro while a group is open");
        }

        playing = true;
        history.beginGroup("macro " + name);
        try {
            for (int i = 0; i < steps.size(); i++) {
                Result r;
                try {
                    r = steps.get(i).run(e);
                } catch (RuntimeException ex) {
                    r = Result.error(String.valueOf(ex.getMessage()));
                }
                if (!r.ok) {
                    history.abortGroup();
                    return Result.error("Macro \"" + name + "\" step " + (i + 1) + " of "
                            + steps.size() + " failed: " + r.message
                            + "; the replay was rolled back");
                }
            }
            history.endGroup();
        } finally {
            playing = false;
            if (history.isGroupOpen()) {
                history.abortGroup(); // never leave a group open
            }
        }
        return Result.ok("Played macro \"" + name + "\" (" + steps.size()
                + (steps.size() == 1 ? " step)" : " steps)"));
    }

    /** @return true between {@link #startRecording} and {@link #stopRecording} */
    public boolean isRecording() {
        return recording != null;
    }

    /** @return true while {@link #play} is running */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * @param name a macro name
     * @return its number of steps, or -1 if there is no such macro
     */
    public int size(String name) {
        List<Action> steps = macros.get(name);
        return steps == null ? -1 : steps.size();
    }

    /** @return the recorded macro names, sorted */
    public List<String> names() {
        return new ArrayList<>(macros.keySet());
    }
}
