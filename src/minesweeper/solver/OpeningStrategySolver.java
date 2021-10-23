package minesweeper.solver;

import minesweeper.analysis.gamestate.GameStateFuzzy;
import minesweeper.gamestate.GameStateModel;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

import java.util.Map;

public class OpeningStrategySolver extends Solver{
    Map<GameStateFuzzy, Location> openingStrategy;

    public OpeningStrategySolver(GameStateModel myGame, SolverSettings preferences, boolean interactive,
                                 Map<GameStateFuzzy, Location> openingStrategy) {
        super(myGame, preferences, interactive);
        this.openingStrategy = openingStrategy;
    }

    @Override
    public Action[] getResult() {
        GameStateFuzzy state = new GameStateFuzzy(getGame());
        if (openingStrategy.containsKey(state)) {
            return new Action[]{new Action(openingStrategy.get(state), Action.CLEAR)};
        }
        return super.getResult();
    }
}
