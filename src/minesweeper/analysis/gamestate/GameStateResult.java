package minesweeper.analysis.gamestate;

import minesweeper.structure.Location;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class GameStateResult {
    public int appeared;
    int won;
    int lost;
    Set<Location> actions;
    GameStateFuzzy gameStateFuzzy;

    public GameStateResult(GameStateFuzzy gameStateFuzzy) {
        this.gameStateFuzzy = gameStateFuzzy;
        this.appeared = 0;
        this.won = 0;
        this.lost = 0;
        this.actions = new HashSet<>();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        DecimalFormat probabilityFormat = new DecimalFormat("#.#");
        builder.append(gameStateFuzzy);
        builder.append("\n\n");
        builder.append("Probability: ");
        builder.append(probabilityFormat.format(100.0 * appeared / 1000000));
        builder.append("%");
        builder.append("\n\n");
        builder.append("Winrate: ");
        builder.append(probabilityFormat.format(100.0 * won / (won + lost)));
        builder.append("%");
        builder.append("\n\n");
        builder.append("Click ");
        boolean isFirstAction = true;
        for (Location action : actions) {
            if (!isFirstAction) builder.append(" or ");
            builder.append("(");
            builder.append(action.x);
            builder.append(", ");
            builder.append(action.y);
            builder.append(")");
            isFirstAction = false;
        }
        builder.append("\n\n");
        return builder.toString();
    }

    /**
     *
     * @param anotherState The state where game needs to be updated.
     * {@code this.gameStateFuzzy.equals(anotherState)} must be {@code true}.
     * @param location Result of the solver in anotherState
     */
    public void addLocation(GameStateFuzzy anotherState, Location location) {
        int symmetry = this.gameStateFuzzy.getSymmetry(anotherState);
        boolean rotate90 = (symmetry & 4) != 0;
        boolean flipRow = (symmetry & 2) != 0;
        boolean flipCol = (symmetry & 1) != 0;

        int x = location.x;
        int y = location.y;
        if (rotate90) {
            int temp = x;
            x = y;
            y = temp;
        }
        if (flipRow) {
            y = this.gameStateFuzzy.height - 1 - y;
        }
        if (flipCol) {
            x = this.gameStateFuzzy.width - 1 - x;
        }
        this.actions.add(new Location(x, y));
    }

    public void addLose() {
        appeared++;
        lost++;
    }

    public void addWin() {
        appeared++;
        won++;
    }

    public static class FrequencyComparator implements Comparator<GameStateResult> {
        @Override
        public int compare(GameStateResult left, GameStateResult right) {
            return Integer.valueOf(right.appeared).compareTo(left.appeared);
        }
    }
}
