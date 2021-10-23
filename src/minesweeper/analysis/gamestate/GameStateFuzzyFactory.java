package minesweeper.analysis.gamestate;

import minesweeper.settings.GameSettings;
import minesweeper.structure.Location;

import java.util.List;

public class GameStateFuzzyFactory {
    public static GameStateFuzzy create(List<List<Integer>> locations, GameSettings settings) {
        int[][] board = new int[settings.width][settings.height];
        for (int col = 0; col < settings.width; col++) {
            for (int row = 0; row < settings.height; row++) {
                board[col][row] = 9;
            }
        }
        for (List<Integer> location : locations) {
            assert(location.size() == 3);
            board[location.get(0)][location.get(1)] = location.get(2);
        }
        return new GameStateFuzzy(settings, board);
    }
}
