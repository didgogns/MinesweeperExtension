package minesweeper.bulk;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.Solver;
import minesweeper.structure.Action;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class ExtendedWorker implements Runnable {

    private boolean stop = false;
    private final ExtendedBulk controller;
    private final int number;

    protected ExtendedWorker(ExtendedBulk controller, int number) {
        this.controller = controller;
        this.number = number;
    }

    @Override
    public void run() {

        System.out.println("worker-" + number + " is starting");

        ExtendedRequest request = controller.getNextRequest(null);
        request.core = this.number;

        while (!stop) {

            if (request.action == ExtendedRequest.BulkAction.STOP) {
                stop = true;
                break;

            } else if (request.action == ExtendedRequest.BulkAction.WAIT) { // wait and then ask again
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                request = controller.getNextRequest(null);
                request.core = this.number;
            } else {

                //System.out.println("Playing game sequence " + request.sequence);
                // play the game
                playGame(request);

                // return it to the controller
                request = controller.getNextRequest(request);
                request.core = this.number;
            }

        }

        System.out.println(Thread.currentThread().getName() + " is stopping");
        controller.consumer.finishThread();
    }

    private void playGame(ExtendedRequest request) {

        int state;

        // if the game is won or lost already then nothing to do.  This can be the case since we don't know what state the Game State model is in.
        if (request.gs.getGameState() == GameStateModel.WON || request.gs.getGameState() == GameStateModel.LOST) {
            return;
        }

        Solver solver = controller.solverFunction.apply(request.gs);

        play: while (true) {

            Action[] moves;
            try {
                solver.start();
                moves = solver.getResult();
            } catch (Exception e) {
                System.out.println("Game " + request.gs.showGameKey() + " has thrown an exception!");
                e.printStackTrace();
                return;
            }

            if (moves.length == 0) {
                System.out.println(request.gs.getSeed() + " - No moves returned by the solver");
                return;
            }

            // play all the moves until all done, or the game is won or lost
            for (Action move : moves) {

                BigDecimal prob = move.getBigProb();

                if (prob.compareTo(BigDecimal.ZERO) <= 0 || prob.compareTo(BigDecimal.ONE) > 0) {
                    System.out.println("Game (" + request.gs.showGameKey() + ") move with probability of " + prob + "! - " + move);
                }

                request.gs.doAction(move);

                state = request.gs.getGameState();

                // only monitor good guesses (brute force, probability engine, zonal, opening book and hooks)
                this.controller.consumer.processAction(request.gs, move, prob, number);

                if (state == GameStateModel.LOST || state == GameStateModel.WON) {
                    break play;
                }
            }
        }
    }

    protected void stop() {
        stop = true;
    }
}
