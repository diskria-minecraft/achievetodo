package io.github.diskria.achievetodo.server;

import org.jetbrains.annotations.Nullable;

public enum AdvancementsMode {

    DEFAULT("bac_advancements", false),
    FIRST_ADVANCEMENTS("bac_advfirst", false),
    TEAM_ADVANCEMENTS("bac_advancements_team", true),
    TOTAL_TEAM_ADVANCEMENTS("bac_advfirst_team_sum", true),
    TEAM_FIRST_ADVANCEMENTS("bac_advfirst_sum", true),
    FIRST_ADVANCEMENTS_IN_TEAM("bac_advfirst_team", true);

    private final String objectiveName;
    private final boolean isTeamsMode;

    AdvancementsMode(String objectiveName, boolean isTeamsMode) {
        this.objectiveName = objectiveName;
        this.isTeamsMode = isTeamsMode;
    }

    public static @Nullable AdvancementsMode findByObjectiveName(String objectiveName) {
        if (objectiveName == null) {
            return null;
        }
        for (AdvancementsMode mode : values()) {
            if (mode.objectiveName.equals(objectiveName)) {
                return mode;
            }
        }
        return null;
    }

    public boolean isTeamsMode() {
        return isTeamsMode;
    }
}
