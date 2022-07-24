package minesweeper.analysis;

import minesweeper.bulk.ExtendedBulk;
import minesweeper.bulk.ExtendedConsumer;
import minesweeper.bulk.ExtendedRequest;
import minesweeper.gamestate.GameStateModel;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.ExtendedSolver;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WinPer3bvAnalysis {
    private static class WinPer3bvAnalysisResult extends ExtendedConsumer {
        public int clicks;
        public int won;
        public Random random;

        public WinPer3bvAnalysisResult(int cores) {
            this.clicks = 0;
            this.won = 0;
            this.cores = cores;
            random = new Random();
        }

        @Override
        public void processRequest(ExtendedRequest request) {
            if (request.gs.getGameState() == GameStateModel.WON) this.won++;
            else if (request.gs.getGameState() != GameStateModel.LOST) {
                System.out.println(request.gs.getGameState());
                // throw exception?
            }
            if (random.nextInt(100000) == 0) System.out.println(print());
        }

        @Override
        public void processAction(GameStateModel model, Action action, BigDecimal probability, int number) {
            this.clicks++;
        }

        @Override
        public String print() {
            return clicks + " clicks made, " + won + " games win.";
        }
    }

    /**
     * Example args: -setting expert -limit 1000000000 -core 8 -seed 195971295
     * @param args
     */
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("setting", true, "Game setting. Difficulty name or in the form of 12x34/56");
        options.addOption("gameType", true, "Game type. If not provided, defaults to standard");
        options.addOption("limit", true, "Number of left clicks to simulate. No flagging.");
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
        long limit = Long.parseLong(cmdline.getOptionValue("limit"));
        SolverSettings preferences = SettingsFactory.GetSettings(SettingsFactory.Setting.SMALL_ANALYSIS);
        long gameGenerator = new Random().nextLong();
        if (cmdline.hasOption("seed")) {
            gameGenerator = Long.parseLong(cmdline.getOptionValue("seed"));
        }
        int workers = 1;
        if (cmdline.hasOption("core")) {
            workers = Integer.parseInt(cmdline.getOptionValue("core"));
        }

        List<Location> corners = Arrays.asList(
                new Location(0, 0),
                new Location(0, gameSettings.height - 1),
                new Location(gameSettings.width - 1, 0),
                new Location(gameSettings.width - 1, gameSettings.height - 1)
        );
        List<Function<GameStateModel, Solver>> solverFunctions = Arrays.asList(
                (GameStateModel model) -> new ExtendedSolver(model, preferences, false),
                (GameStateModel model) -> new ExtendedSolver(model, preferences, false) {
                    @Override
                    public FinalMoves doNewProcess() {
                        return new FinalMoves(super.doNewProcess().result[0]);
                    }
                },
                (GameStateModel model) -> new ExtendedSolver(model, preferences, false) {
                    @Override
                    public FinalMoves doNewProcess() {
                        // Open 4 corners first.
                        GameStateModel model = this.getGame();
                        if (model.getActionCount() == 0) {
                            Action[] cornerMoves = corners.stream()
                                    .map((Location location) -> (new Action(location, Action.CLEAR)))
                                    .toArray(Action[]::new);
                            return new FinalMoves(cornerMoves);
                        }
                        return new FinalMoves(super.doNewProcess().result[0]);
                    }
                }
        );
        List<String> results = new ArrayList<>();

        for (Function<GameStateModel, Solver> solver : solverFunctions) {
            ExtendedBulk bulk = new ExtendedBulk(gameGenerator, (ExtendedConsumer consumer) -> {
                if (!(consumer instanceof WinPer3bvAnalysisResult)) {
                    return false;
                }
                WinPer3bvAnalysisResult analysisResult = (WinPer3bvAnalysisResult) consumer;
                return (analysisResult.clicks >= limit);
            }, gameType, gameSettings, solver, workers);
            bulk.registerConsumer(new WinPer3bvAnalysisResult(workers));
            bulk.run();
            try {
                results.add(bulk.consumer.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (String result : results) System.out.println(result);
    }
}
