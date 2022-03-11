package minesweeper.analysis;

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
import minesweeper.util.CommandLineUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.math.BigDecimal;
import java.util.Random;

public class WinPer3bvAnalysis {
    private static class WinPer3bvAnalysisResult extends ExtendedConsumer {
        public int clicks;
        public int won;
        public Random random;

        public WinPer3bvAnalysisResult() {
            this.clicks = 0;
            this.won = 0;
            random = new Random();
        }

        @Override
        public void processRequest(ExtendedRequest request) {
            if (request.gs.getGameState() == GameStateModel.WON) this.won++;
            else if (request.gs.getGameState() != GameStateModel.LOST) {
                System.out.println(request.gs.getGameState());
                // throw exception?
            }
            if (random.nextInt(100000) == 0) print();
        }

        @Override
        public void processAction(Action action, BigDecimal probability, boolean isSafe) {
            this.clicks++;
        }

        @Override
        public void print() {
            System.out.println(clicks + " clicks made, " + won + " games win.");
        }
    }

    /**
     * Example args: -setting expert -limit 1000000000 -seed 195971295
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
        ExtendedBulk bulk = new ExtendedBulk(gameGenerator, (ExtendedConsumer consumer) -> {
            if (!(consumer instanceof WinPer3bvAnalysisResult)) {
                return false;
            }
            WinPer3bvAnalysisResult analysisResult = (WinPer3bvAnalysisResult) consumer;
            return (analysisResult.clicks >= limit);
        }, gameType, gameSettings, (GameStateModel model) -> new Solver(model, preferences, false), workers);
        bulk.registerConsumer(new WinPer3bvAnalysisResult());
        bulk.run();
        System.out.println(bulk.getDuration());
    }
}
