package minesweeper.analysis;

import minesweeper.analysis.gamestate.GameStateFuzzy;
import minesweeper.analysis.gamestate.GameStateResult;
import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.random.DefaultRNG;
import minesweeper.random.RNG;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.Solver;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

import java.util.*;

public class MinesweeperCornerStrategyAnalyzer {
    private static boolean playGame(GameStateModel gs, Solver solver) {
        int state = gs.getGameState();

        while(state != 2 && state != 3) {
            Action[] moves;
            try {
                solver.start();
                moves = solver.getResult();
            } catch (Exception var5) {
                System.out.println("Game " + gs.showGameKey() + " has thrown an exception!");
                return false;
            }

            if (moves.length == 0) {
                System.err.println("No moves returned by the solver for game " + gs.showGameKey());
                return false;
            }

            for(int i = 0; i < moves.length; ++i) {
                gs.doAction(moves[i]);
                state = gs.getGameState();
                if (state == 2 || state == 3) {
                    return state != 2;
                }
            }
        }

        return state != 2;
    }

    public static void run(int gamesMax, GameSettings gameSettings, GameType gameType, Long gameGenerator, SolverSettings preferences, boolean clearCorners) {
        System.out.println("At BulkRunner run method");
        int steps = 0;
        int wins = 0;
        RNG seeder = DefaultRNG.getRNG(gameGenerator);

        while(steps < gamesMax) {
            ++steps;
            GameStateModel gs = null;
            if (!clearCorners) {
                gs = GameFactory.create(gameType, gameSettings, seeder.random(0));
            } else {
                label36:
                while(true) {
                    while(true) {
                        if (gs != null) {
                            break label36;
                        }

                        gs = GameFactory.create(gameType, gameSettings, seeder.random(0));
                        List<Action> preactions = new ArrayList();
                        preactions.add(new Action(0, 0, 1));
                        preactions.add(new Action(0, gameSettings.height - 1, 1));
                        preactions.add(new Action(gameSettings.width - 1, 0, 1));
                        preactions.add(new Action(gameSettings.width - 1, gameSettings.height - 1, 1));
                        Iterator var12 = preactions.iterator();

                        while(var12.hasNext()) {
                            Action action = (Action)var12.next();
                            gs.doAction(action);
                            int state = gs.getGameState();
                            if (state == 2 || state == 3) {
                                gs = null;
                                break;
                            }
                        }
                    }
                }
            }

            Solver solver = new Solver(gs, preferences, false);
            boolean win = playGame(gs, solver);
            if (win) {
                ++wins;
            }
        }

        System.out.println("BulkRunner run method ending, " + wins + "/" + steps);
    }

    public static void main(String[] args) {
        GameSettings gameSettings = GameSettings.create(60, 60, 900);
        GameType gameType = GameType.STANDARD;
        SolverSettings preferences = SettingsFactory.GetSettings(SettingsFactory.Setting.SMALL_ANALYSIS);
        int gamesMax = 10000;
        Long gameGenerator = (new Random()).nextLong();
        run(gamesMax, gameSettings, gameType, gameGenerator, preferences, true);
    }
}
