package minesweeper.bulk;

import minesweeper.structure.Action;

import java.math.BigDecimal;

/**
 * Saves important information from games.
 */
public abstract class ExtendedConsumer {
    public abstract void processRequest(ExtendedRequest request);
    public abstract void processAction(Action action, BigDecimal probability, boolean isSafe);

    public abstract void print();
}
