package minesweeper.bulk;

import minesweeper.gamestate.GameStateModel;

public class ExtendedRequest {

    protected final static ExtendedRequest WAIT = new ExtendedRequest() {
        {
            action = BulkAction.WAIT;
        }
    };

    protected final static ExtendedRequest STOP = new ExtendedRequest() {
        {
            action = BulkAction.STOP;
        }
    };

    public enum BulkAction {
        STOP,
        WAIT,
        RUN
    }

    protected BulkAction action;
    protected int sequence;    // the sequence number for this request
    protected int slot;        // the slot the request is to be store in the buffer
    protected GameStateModel gs;
}
