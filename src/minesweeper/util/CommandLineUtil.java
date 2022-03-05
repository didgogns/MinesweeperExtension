package minesweeper.util;

import minesweeper.settings.GameSettings;
import minesweeper.settings.GameType;

import java.util.HashMap;
import java.util.Map;

public class CommandLineUtil {
    private static Map<String, GameSettings> NAMED_SETTINGS = new HashMap<>();
    private static Map<String, GameType> NAMED_TYPES = new HashMap<>();
    static {
        NAMED_SETTINGS.put("beginner", GameSettings.BEGINNER);
        NAMED_SETTINGS.put("intermediate", GameSettings.ADVANCED);
        NAMED_SETTINGS.put("expert", GameSettings.EXPERT);

        for (GameType type : GameType.values()) {
            NAMED_TYPES.put(type.name, type);
        }
    }

    public static GameSettings settingsFromString(String input) {
        input = input.toLowerCase();
        if (NAMED_SETTINGS.containsKey(input)) {
            return NAMED_SETTINGS.get(input);
        }
        int firstSeparator = input.indexOf("x");
        assert firstSeparator > 0: "Game setting must contain 'x' character";
        int secondSeparator = input.indexOf("/");
        assert(secondSeparator > 0): "Game setting must contain '/' character";
        assert(secondSeparator > firstSeparator): "In game setting, 'x' must precede '/' character";
        int width = Integer.parseInt(input.substring(0, firstSeparator));
        int height = Integer.parseInt(input.substring(firstSeparator + 1, secondSeparator));
        int mines = Integer.parseInt(input.substring(secondSeparator + 1));
        return GameSettings.create(width, height, mines);
    }

    public static GameType typesFromString(String input) {
        input = input.toLowerCase();
        assert(NAMED_TYPES.containsKey(input)): "Game type must be one of existing game types";
        return NAMED_TYPES.get(input);
    }
}
