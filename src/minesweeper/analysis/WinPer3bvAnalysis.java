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

import java.util.Random;

public class WinPer3bvAnalysis {
    private static class WinPer3bvAnalysisResult {
        public int clicks;
        public boolean won;

        public WinPer3bvAnalysisResult(int clicks, boolean won) {
            this.clicks = clicks;
            this.won = won;
        }
    }

    private static WinPer3bvAnalysisResult playGame(GameStateModel gs, Solver solver) {
        int state = gs.getGameState();
        int clicks = 0;

        while (state != GameStateModel.LOST && state != GameStateModel.WON) {
            Action[] moves;
            try {
                solver.start();
                moves = solver.getResult();
            } catch (Exception e) {
                System.out.println("Game " + gs.showGameKey() + " has thrown an exception!");
                return new WinPer3bvAnalysisResult(clicks, false);
            }

            if (moves.length == 0) {
                System.err.println("No moves returned by the solver for game " + gs.showGameKey());
                return new WinPer3bvAnalysisResult(clicks, false);
            }

            for (Action move : moves) {
                gs.doAction(move);
                clicks++;
                state = gs.getGameState();
                if (state == GameStateModel.LOST || state == GameStateModel.WON) {
                    new WinPer3bvAnalysisResult(clicks, state == GameStateModel.WON);
                }
            }
        }

        return new WinPer3bvAnalysisResult(clicks, state == GameStateModel.WON);
    }

    public static void run(long limit, GameSettings gameSettings, GameType gameType, Long gameGenerator, SolverSettings preferences) {
        long clickCnt = 0;
        long win = 0;
        RNG seeder = DefaultRNG.getRNG(gameGenerator);
        while (clickCnt < limit) {
            GameStateModel gs = GameFactory.create(gameType, gameSettings, seeder.random(0));
            Solver solver = new Solver(gs, preferences, false);
            WinPer3bvAnalysisResult result = playGame(gs, solver);
            clickCnt += result.clicks;
            if (result.won) win++;
        }
        System.out.println(clickCnt + " clicks made, " + win + " games win.");
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
            System.out.println(cmdline.getOptionValue("seed"));
            gameGenerator = Long.parseLong(cmdline.getOptionValue("seed"));
        }

        run(limit, gameSettings, gameType, gameGenerator, preferences);
    }
}
