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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class MinesweeperCornerStrategyAnalyzer {
    private static class CornerStrategyAnalysisResult extends ExtendedConsumer {
        public int games;
        public int won;
        public Random random;

        public CornerStrategyAnalysisResult(int cores) {
            this.games = 0;
            this.won = 0;
            this.cores = cores;
            random = new Random();
        }

        @Override
        public void processRequest(ExtendedRequest request) {
            this.games++;
            if (request.gs.getGameState() == GameStateModel.WON) this.won++;
            else if (request.gs.getGameState() != GameStateModel.LOST) {
                System.out.println(request.gs.getGameState());
                // throw exception?
            }
            if (random.nextInt(100000) == 0) System.out.println(print());
        }

        @Override
        public void processAction(GameStateModel model, Action action, BigDecimal probability, int number) {
        }

        @Override
        public String print() {
            return won + " / " + games;
        }
    }

    /**
     * Example args: -setting 60x60/900 -gamesMax 10000 -clearCorners -core 4
     * @param args: command line arguments
     */
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("setting", true, "Game setting. Difficulty name or in the form of 12x34/56");
        options.addOption("gameType", true, "Game type. If not provided, defaults to standard");
        options.addOption("gamesMax", true, "Number of games to simulate.");
        options.addOption("clearCorners", false, "If given, clears corners first.");
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
        SolverSettings preferences = SettingsFactory.GetSettings(SettingsFactory.Setting.SMALL_ANALYSIS);
        boolean clearCorners = cmdline.hasOption("clearCorners");
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
        if (!clearCorners) {
            corners.clear();
        }
        ExtendedBulk bulk = new ExtendedBulk(gameGenerator, (ExtendedConsumer consumer) -> {
            assert consumer instanceof CornerStrategyAnalysisResult;
            CornerStrategyAnalysisResult analysisResult = (CornerStrategyAnalysisResult) consumer;
            return (analysisResult.games >= gamesMax);
        }, gameType, gameSettings, (GameStateModel model) -> new Solver(model, preferences, false), workers);
        bulk.setPreActions(corners.stream().
                map((Location location) -> (new Action(location, Action.CLEAR)))
                .collect(Collectors.toList())
        );
        bulk.registerConsumer(new CornerStrategyAnalysisResult(workers));
        bulk.run();
    }
}
