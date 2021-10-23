package minesweeper.analysis;

import minesweeper.analysis.gamestate.GameStateFuzzy;
import minesweeper.analysis.gamestate.GameStateFuzzyFactory;
import minesweeper.analysis.gamestate.GameStateResult;
import minesweeper.gamestate.GameFactory;
import minesweeper.gamestate.GameStateModel;
import minesweeper.random.DefaultRNG;
import minesweeper.random.RNG;
import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;
import minesweeper.solver.OpeningStrategySolver;
import minesweeper.solver.Solver;
import minesweeper.solver.settings.SettingsFactory;
import minesweeper.solver.settings.SolverSettings;
import minesweeper.structure.Action;
import minesweeper.structure.Location;

import java.math.BigInteger;
import java.util.*;

public class MinesweeperCommonStateAnalyzer {
    private static boolean playGame(GameStateModel gs, Solver solver, Map<GameStateFuzzy, GameStateResult> frequencyMap) {

        int state = gs.getGameState();

        List<GameStateFuzzy> states = new ArrayList<>();
        List<Location> fuzzyStateActions = new ArrayList<>();

        play: while (true) {
            if (state == GameStateModel.LOST || state == GameStateModel.WON) {
                break play;
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
            for (int i = 0; i < moves.length; i++) {
                gs.doAction(moves[i]);
                state = gs.getGameState();
                if (state == GameStateModel.LOST || state == GameStateModel.WON) {
                    break play;
                }
            }
        }

        for (int i = 0; i < states.size(); i++) {
            GameStateFuzzy fuzzyState = states.get(i);
            Location location = fuzzyStateActions.get(i);

            if (!frequencyMap.containsKey(fuzzyState)) {
                frequencyMap.put(fuzzyState, new GameStateResult(fuzzyState));
            }
            frequencyMap.get(fuzzyState).addLocation(location);
            if (state == GameStateModel.LOST) {
                frequencyMap.get(fuzzyState).addLose(location);
            }
            else {
                frequencyMap.get(fuzzyState).addWin(location);
            }
        }

        if (state == GameStateModel.LOST) {
            return false;
        }
        else {
            return true;
        }
    }

    public static void run(int gamesMax, GameSettings gameSettings, GameType gameType, Long gameGenerator,
                           SolverSettings preferences) {
        System.out.println("At BulkRunner run method");

        int steps = 0;
        int wins = 0;

        RNG seeder = DefaultRNG.getRNG(gameGenerator);

        Map<GameStateFuzzy, GameStateResult> frequencyMap = new HashMap<>();

        while (steps < gamesMax) {

            steps++;
            GameStateModel gs = GameFactory.create(gameType, gameSettings, seeder.random(0));

            Map<GameStateFuzzy, Location> locationMap = new HashMap<>();
            locationMap.put(GameStateFuzzyFactory.create(Arrays.asList(Arrays.asList(0, 0, 1)), gameSettings),
                    new Location(gameSettings.width - 1, gameSettings.height - 1));
            locationMap.put(GameStateFuzzyFactory.create(Arrays.asList(Arrays.asList(0, 0, 2)), gameSettings),
                    new Location(gameSettings.width - 1, gameSettings.height - 1));

            Solver solver = new OpeningStrategySolver(gs, preferences, false, locationMap);

            boolean win = playGame(gs, solver, frequencyMap);

            if (win) {
                wins++;
            }

        }
        List<GameStateResult> frequentStates = new ArrayList<>(frequencyMap.values());
        frequentStates.sort(new GameStateResult.FrequencyComparator());

        for (GameStateResult state : frequentStates) {
            if ((double) state.appeared * state.appeared < gamesMax) break;
            System.out.println(state);
        }
        System.out.println(wins);
    }

    public static void main(String[] args) {
        GameSettings gameSettings = GameSettings.EXPERT;
        GameType gameType = GameType.STANDARD;
        SolverSettings preferences = SettingsFactory.GetSettings(SettingsFactory.Setting.SMALL_ANALYSIS);
        int gamesMax = 1000000;
        Long gameGenerator = new Random().nextLong();
        run(gamesMax, gameSettings, gameType, gameGenerator, preferences);
    }
}
