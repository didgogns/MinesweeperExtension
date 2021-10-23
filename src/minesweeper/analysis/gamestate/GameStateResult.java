package minesweeper.analysis.gamestate;

import minesweeper.structure.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class GameStateResult {
    public int appeared;
    int won;
    int lost;
    Map<Location, Integer> actions;
    GameStateFuzzy gameStateFuzzy;

    public GameStateResult(GameStateFuzzy gameStateFuzzy) {
        this.gameStateFuzzy = gameStateFuzzy;
        this.appeared = 0;
        this.won = 0;
        this.lost = 0;
        this.actions = new HashMap<>();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(gameStateFuzzy);
        builder.append(actions);
        builder.append("\n");
        builder.append("Occurred ");
        builder.append(appeared);
        builder.append(" times, won ");
        builder.append(won);
        builder.append(" times, lost ");
        builder.append(lost);
        builder.append(" times");
        builder.append("\n");
        return builder.toString();
    }

    public static class FrequencyComparator implements Comparator<GameStateResult> {
        @Override
        public int compare(GameStateResult left, GameStateResult right) {
            return Integer.valueOf(right.appeared).compareTo(left.appeared);
        }
    }

    public void addLocation(Location location) {
        if (!actions.containsKey(location)) {
            actions.put(location, 0);
        }
        actions.put(location, actions.get(location) + 1);
    }

    public void addLose(Location location) {
        appeared++;
        lost++;
    }

    public void addWin(Location location) {
        appeared++;
        won++;
    }
}
