package com.example.RuneBotApi.WorldWalker.pathfinder;

public enum Spellbook {
    STANDARD,
    ANCIENTS,
    LUNARS,
    ARCEUUS;

    public static Spellbook fromString(String name) {
        for (Spellbook spellbook : Spellbook.values()) {
            if (spellbook.name().toLowerCase().equals(name.toLowerCase())) {
                return spellbook;
            }
        }

        return STANDARD;
    }

    public static Spellbook fromVarbit(int varbit) {
        if (varbit >= Spellbook.values().length) {
            return Spellbook.STANDARD;
        }

        return Spellbook.values()[varbit];
    }
}
