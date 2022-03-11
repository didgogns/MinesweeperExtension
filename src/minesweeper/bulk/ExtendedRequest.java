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

    public BulkAction action;
    public int sequence;    // the sequence number for this request
    public int slot;        // the slot the request is to be store in the buffer
    public GameStateModel gs;
}
