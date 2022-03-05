package minesweeper.analysis;

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
import minesweeper.util.CommandLineUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MinesweeperCornerStrategyAnalyzer {
    private static boolean playGame(GameStateModel gs, Solver solver) {
        int state = gs.getGameState();

        while (state != GameStateModel.LOST && state != GameStateModel.WON) {
            Action[] moves;
            try {
                solver.start();
                moves = solver.getResult();
            } catch (Exception e) {
                System.out.println("Game " + gs.showGameKey() + " has thrown an exception!");
                return false;
            }

            if (moves.length == 0) {
                System.err.println("No moves returned by the solver for game " + gs.showGameKey());
                return false;
            }

            for (Action move : moves) {
                gs.doAction(move);
                state = gs.getGameState();
                if (state == GameStateModel.LOST || state == GameStateModel.WON) {
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
            GameStateModel gs;
            if (!clearCorners) {
                gs = GameFactory.create(gameType, gameSettings, seeder.random(0));
            } else {

                gs = GameFactory.create(gameType, gameSettings, seeder.random(0));
                List<Action> preActions = new ArrayList<>();
                preActions.add(new Action(0, 0, 1));
                preActions.add(new Action(0, gameSettings.height - 1, 1));
                preActions.add(new Action(gameSettings.width - 1, 0, 1));
                preActions.add(new Action(gameSettings.width - 1, gameSettings.height - 1, 1));

                for (Action action : preActions) {
                    gs.doAction(action);
                    int state = gs.getGameState();
                    if (state == GameStateModel.LOST || state == GameStateModel.WON) {
                        steps--;
                        gs = null;
                        break;
                    }
                }
            }
            if (gs == null) continue;

            Solver solver = new Solver(gs, preferences, false);
            boolean win = playGame(gs, solver);
            if (win) {
                ++wins;
            }
        }

        System.out.println("BulkRunner run method ending, " + wins + "/" + steps);
    }

    /**
     * Example args: -setting 60x60/900 -gamesMax 10000 -clearCorners
     * @param args
     */
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("setting", true, "Game setting. Difficulty name or in the form of 12x34/56");
        options.addOption("gameType", true, "Game type. If not provided, defaults to standard");
        options.addOption("gamesMax", true, "Number of games to simulate.");
        options.addOption("clearCorners", false, "If given, clears corners first.");
        options.addOption("seed", true, "RNG seed. If not provided, default seed is used.");

        CommandLine cmdline;
        try {
            cmdline = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        GameSettings gameSettings = CommandLineUtil.settingsFromString(cmdline.getOptionValue("setting"));
        GameType gameType = GameType.STANDARD;
        if (cmdline.hasOption("gameType")) {
            gameType = CommandLineUtil.typesFromString(cmdline.getOptionValue("gameType"));
        }
        // Total number of simulations
        int gamesMax = Integer.parseInt(cmdline.getOptionValue("gamesMax"));
        SolverSettings preferences = SettingsFactory.GetSettings(SettingsFactory.Setting.SMALL_ANALYSIS);
        boolean clearCorners = cmdline.hasOption("clearCorners");
        long gameGenerator = new Random().nextLong();
        if (cmdline.hasOption("seed")) {
            gameGenerator = Long.parseLong(cmdline.getOptionValue("seed"));
        }
        run(gamesMax, gameSettings, gameType, gameGenerator, preferences, clearCorners);
    }
}
