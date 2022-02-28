package minesweeper.solver;

import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Goal: Make solver which acts like good human player playing high difficulty boards.
 * That is,
 *   - Play 1 move at a time. Preferably do not move cursor much and try to click closest to edge.
 *   (easy to implement)
 *   - Guess 50/50 or similar fixed patterns as soon as it discovers (will be VERY hard to implement!)
 */

public class HumanlySolver extends Solver {
    public HumanlySolver(GameStateModel myGame, SolverSettings preferences, boolean interactive) {
        super(myGame, preferences, interactive);
    }

    public double distanceFromEdge(Location location) {
        int height = this.getGame().getHeight();
        int width = this.getGame().getWidth();
        int resultHeight = min(height - 1 - location.y, location.y);
        int resultWidth = min(width - 1 - location.x, location.x);
        return min(resultHeight, resultWidth) + 0.0001 * max(resultHeight, resultWidth);
    }

    @Override
    public Action[] getResult() {
        Action[] candidates = super.getResult();
        Action bestCandidate = null;
        double bestResult = 0;
        for (Action candidate : candidates) {
            double result = distanceFromEdge(candidate);
            if (bestCandidate == null || result < bestResult) {
                bestResult = result;
                bestCandidate = candidate;
            }
        }
        return new Action[]{bestCandidate};
    }
}
