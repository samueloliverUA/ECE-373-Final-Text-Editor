package edu.arizona.ece373.editor.cli;

import edu.arizona.ece373.editor.app.Action;

/**
 * Creates the {@link Action} for one verb from its parsed arguments. {@link CommandParser}
 * holds one per verb, registered as a lambda (Factory pattern).
 */
@FunctionalInterface
public interface ActionFactory {

    /**
     * Builds the action for one verb from its parsed arguments.
     *
     * @param args the arguments after the verb
     * @return the action to run
     * @throws ParseException if the arguments are missing or malformed
     */
    Action create(Tokens args) throws ParseException;
}
