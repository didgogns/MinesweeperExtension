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
import minesweeper.util.CommandLineUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class MinesweeperCommonStateAnalyzer {
    private static boolean playGame(
            GameStateModel gs,
            Solver solver,
            Map<GameStateFuzzy, GameStateResult> frequencyMap,
            int steps,
            int gamesMax,
            int target
    ) {

        int state = gs.getGameState();

        List<GameStateFuzzy> states = new ArrayList<>();
        List<Location> fuzzyStateActions = new ArrayList<>();

        play: while (true) {
            if (state == GameStateModel.LOST || state == GameStateModel.WON) {
                break;
            }

            Action[] moves;
            try {
                solver.start();
                moves = solver.getResult();
            } catch (Exception e) {
                System.out.println("Game " + gs.showGameKey() + " has thrown an exception!");
                return false;
            }

            if (moves.length == 1 && !moves[0].isCertainty()) {
                GameStateFuzzy fuzzyState = new GameStateFuzzy(gs);
                states.add(fuzzyState);
                fuzzyStateActions.add(moves[0]);
            }

            if (moves.length == 0) {
                System.err.println("No moves returned by the solver for game " + gs.showGameKey());
                return false;
            }

            // play all the moves until all done, or the game is won or lost
            for (Action move : moves) {
                gs.doAction(move);
                state = gs.getGameState();
                if (state == GameStateModel.LOST || state == GameStateModel.WON) {
                    break play;
                }
            }
        }

        for (int i = 0; i < states.size(); i++) {
            GameStateFuzzy fuzzyState = states.get(i);
            Location location = fuzzyStateActions.get(i);

            if (!frequencyMap.containsKey(fuzzyState) && steps < gamesMax / target * 10) {
                frequencyMap.put(fuzzyState, new GameStateResult(fuzzyState));
            }
            if (frequencyMap.containsKey(fuzzyState)) {
                frequencyMap.get(fuzzyState).addLocation(fuzzyState, location);
                if (state == GameStateModel.LOST) {
                    frequencyMap.get(fuzzyState).addLose();
                }
                else {
                    frequencyMap.get(fuzzyState).addWin();
                }
            }
        }

        return state != GameStateModel.LOST;
    }

    public static void run(
            GameSettings gameSettings,
            GameType gameType,
            Long gameGenerator,
            SolverSettings preferences,
            int gamesMax,
            int target,
            double sigma
    ) {
        System.out.println("At BulkRunner run method");

        int steps = 0;
        int wins = 0;

        RNG seeder = DefaultRNG.getRNG(gameGenerator);

        Map<GameStateFuzzy, GameStateResult> frequencyMap = new HashMap<>();

        while (steps < gamesMax) {

            steps++;
            GameStateModel gs = GameFactory.create(gameType, gameSettings, seeder.random(0));

            Solver solver = new Solver(gs, preferences, false);

            boolean win = playGame(gs, solver, frequencyMap, steps, gamesMax, target);

            if (win) {
                wins++;
            }

        }
        List<GameStateResult> frequentStates = new ArrayList<>(frequencyMap.values());
        frequentStates.sort(new GameStateResult.FrequencyComparator());

        double mean = 1.0 * gamesMax / target;
        double stdev = Math.sqrt(mean * (1 - 1.0 / target));
        double limit = mean - sigma * stdev;

        for (GameStateResult state : frequentStates) {
            if (state.appeared < limit) break;
            System.out.println(state);
        }
        System.out.println(wins);
    }

    /**
     * Example args: -setting intermediate -gamesMax 1000000 -target 1000
     * @param args
     */
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("setting", true, "Game setting. Difficulty name or in the form of 12x34/56");
        options.addOption("gameType", true, "Game type. If not provided, defaults to standard");
        options.addOption("gamesMax", true, "Number of games to simulate.");
        options.addOption("target", true, "It saves all game state with gamesMax / target occurrences.");
        options.addOption("sigma", true, "Standard deviation, default is 4.");
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
        // Extraction target: if 1000, every state with 1 in 1000 or more chance will be extracted
        int target = Integer.parseInt(cmdline.getOptionValue("target"));
        double sigma = 4.0;
        if (cmdline.hasOption("sigma")) {
            sigma = Double.parseDouble(cmdline.getOptionValue("sigma"));
        }
        SolverSettings preferences = SettingsFactory.GetSettings(SettingsFactory.Setting.SMALL_ANALYSIS);
        long gameGenerator = new Random().nextLong();
        if (cmdline.hasOption("seed")) {
            gameGenerator = Long.parseLong(cmdline.getOptionValue("seed"));
        }

        run(gameSettings, gameType, gameGenerator, preferences, gamesMax, target, sigma);
    }
}
