package minesweeper.bulk;

import minesweeper.gamestate.GameStateModel;
import minesweeper.structure.Action;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/**
 * Saves important information from games.
 */
public abstract class ExtendedConsumer extends CompletableFuture<String> {
    public abstract void processRequest(ExtendedRequest request);
    public abstract void processAction(GameStateModel model, Action action, BigDecimal probability, int number);
    public abstract String print();

    public void finishThread() {
        cores--;
        if (cores == 0) {
            complete(print());
        }
    }
    public int cores;
}
