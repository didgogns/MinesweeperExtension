package minesweeper.analysis.gamestate;

import minesweeper.gamestate.GameStateModel;
import minesweeper.settings.GameSettings;
import minesweeper.structure.Location;

/**
 * Minesweeper game state.
 * The most common game state is "Initial state", followed by "Corner 1".
 *
 * @author didgogns
 */

public class GameStateFuzzy {
    protected final int[][] board;
    protected final int width;
    protected final int height;
    protected final int mines;
    Integer hashCode;

    public GameStateFuzzy(GameStateModel model) {
        this.width = model.getWidth();
        this.height = model.getHeight();
        this.mines = model.getMines();
        this.board = new int[width][height];
        for (int col = 0; col < width; col++) {
            for (int row = 0; row < height; row++) {
                Location location = new Location(col, row);
                board[col][row] = model.query(location);
                if (board[col][row] < 0 || board[col][row] > 8) {
                    board[col][row] = 9;
                }
            }
        }
        hashCode = null;
    }

    public GameStateFuzzy(GameSettings gamesettings, int[][] board) {
        this.width = gamesettings.width;
        this.height = gamesettings.height;
        this.mines = gamesettings.mines;
        assert (board.length == width);
        assert (board[0].length == height);
        this.board = board;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final GameStateFuzzy other = (GameStateFuzzy) obj;
        if (this.hashCode() != other.hashCode()) return false;
        if (this.width != other.width) return false;
        if (this.height != other.height) return false;
        if (this.mines != other.mines) return false;

        int iterations = 4;
        if (this.width == this.height) iterations = 8;

        for (int i = 0; i < iterations; i++) {
            if (compare(other.board, (i & 4) != 0, (i & 2) != 0, (i & 1) != 0)) {
                return true;
            }
        }
        return false;
    }

    public int getSymmetry(GameStateFuzzy other) {
        int iterations = 4;
        if (this.width == this.height) iterations = 8;

        for (int i = 0; i < iterations; i++) {
            if (compare(other.board, (i & 4) != 0, (i & 2) != 0, (i & 1) != 0)) {
                return i;
            }
        }
        throw new IllegalArgumentException("GameStateFuzzy other must be equal to this!");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("```\n");
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                builder.append(board[col][row]);
            }
            builder.append("\n");
        }
        builder.append("```");
        return builder.toString();
    }

    private boolean compare(int[][] other, boolean rotate90, boolean flipRow, boolean flipCol) {
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int targetRow = row;
                int targetCol = col;
                if (flipRow) targetRow = height - 1 - row;
                if (flipCol) targetCol = width - 1 - col;
                if (rotate90) {
                    // throw error when height != width?
                    int temp = targetRow;
                    targetRow = targetCol;
                    targetCol = temp;
                }
                if (board[col][row] != other[targetCol][targetRow]) return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (this.hashCode != null) return hashCode;
        int result = 0;

        int iterations = 4;
        if (this.width == this.height) iterations = 8;

        for (int i = 0; i < iterations; i++) {
            boolean rotate90 = (i & 4) != 0;
            boolean flipRow = (i & 2) != 0;
            boolean flipCol = (i & 1) != 0;

            int tempHash = 0;
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    int targetRow = row;
                    int targetCol = col;
                    if (flipRow) targetRow = height - 1 - row;
                    if (flipCol) targetCol = width - 1 - col;
                    if (rotate90) {
                        // throw error when height != width?
                        int temp = targetRow;
                        targetRow = targetCol;
                        targetCol = temp;
                    }
                    tempHash = tempHash * 31 + board[targetCol][targetRow];
                }
            }

            result ^= tempHash;
        }
        hashCode = result;
        return result;
    }
}
