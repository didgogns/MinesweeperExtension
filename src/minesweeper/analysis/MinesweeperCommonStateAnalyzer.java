package minesweeper.analysis;

import minesweeper.analysis.gamestate.GameStateFuzzy;
import minesweeper.analysis.gamestate.GameStateResult;
import minesweeper.bulk.ExtendedBulk;
import minesweeper.bulk.ExtendedConsumer;
import minesweeper.bulk.ExtendedRequest;
import minesweeper.gamestate.GameStateModel;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class MinesweeperCommonStateAnalyzer {
    private static class CommonStateAnalysisResult extends ExtendedConsumer {
        Map<GameStateFuzzy, GameStateResult> frequencyMap = new HashMap<>();
        Map<Long, List<GameStateFuzzy>> states;
        Map<Long, List<Location>> fuzzyStateActions;
        int games = 0;
        final int gamesMax;
        final int target;
        final double sigma;

        CommonStateAnalysisResult(int gamesMax, int target, double sigma, int workers) {
            this.gamesMax = gamesMax;
            this.target = target;
            this.sigma = sigma;

            states = new HashMap<>();
            fuzzyStateActions = new HashMap<>();
        }

        @Override
        public void processRequest(ExtendedRequest request) {
            int state = request.gs.getGameState();
            long seed = request.gs.getSeed();
            List<GameStateFuzzy> coreStates = states.get(seed);
            List<Location> coreActions = fuzzyStateActions.get(seed);
            if (coreStates == null && coreActions == null) {
                games++;
                return;
            }

            for (int i = 0; i < coreStates.size(); i++) {
                GameStateFuzzy fuzzyState = coreStates.get(i);
                Location location = coreActions.get(i);

                if (!frequencyMap.containsKey(fuzzyState) && games < gamesMax / target * 10) {
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

            states.remove(seed);
            fuzzyStateActions.remove(seed);
            games++;
        }

        @Override
        public void processAction(GameStateModel model, Action action, BigDecimal probability, int number) {
            if (!action.isCertainty()) {
                long seed = model.getSeed();
                if (!states.containsKey(seed)) {
                    states.put(seed, new ArrayList<>());
                    fuzzyStateActions.put(seed, new ArrayList<>());
                }
                states.get(seed).add(new GameStateFuzzy(model));
                fuzzyStateActions.get(seed).add(action);
            }
        }

        @Override
        public String print() {
            StringBuilder builder = new StringBuilder();
            List<GameStateResult> frequentStates = new ArrayList<>(frequencyMap.values());
            frequentStates.sort(new GameStateResult.FrequencyComparator());

            double mean = 1.0 * gamesMax / target;
            double stdev = Math.sqrt(mean * (1 - 1.0 / target));
            double limit = mean - sigma * stdev;

            for (GameStateResult state : frequentStates) {
                if (state.appeared < limit) break;
                builder.append(state);
            }
            return builder.toString();
        }
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
        options.addOption("core", true, "Number of cores to use. Default is 1.");

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
        int workers = 1;
        if (cmdline.hasOption("core")) {
            workers = Integer.parseInt(cmdline.getOptionValue("core"));
        }

        ExtendedBulk bulk = new ExtendedBulk(gameGenerator, (ExtendedConsumer consumer) -> {
            assert consumer instanceof CommonStateAnalysisResult;
            CommonStateAnalysisResult analysisResult = (CommonStateAnalysisResult) consumer;
            return (analysisResult.games >= gamesMax);
        }, gameType, gameSettings, (GameStateModel model) -> new Solver(model, preferences, false), workers);
        bulk.registerConsumer(new CommonStateAnalysisResult(gamesMax, target, sigma, workers));
        bulk.run();
    }
}
