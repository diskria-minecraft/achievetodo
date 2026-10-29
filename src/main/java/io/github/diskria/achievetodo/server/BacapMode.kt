package io.github.diskria.achievetodo.server

enum class BacapMode(val objectiveName: String, val isTeamsMode: Boolean = false) {

    DEFAULT("bac_advancements"),
    FIRST_ADVANCEMENTS("bac_advfirst"),
    TEAM_ADVANCEMENTS("bac_advancements_team", isTeamsMode = true),
    TOTAL_TEAM_ADVANCEMENTS("bac_advfirst_team_sum", isTeamsMode = true),
    TEAM_FIRST_ADVANCEMENTS("bac_advfirst_sum", isTeamsMode = true),
    FIRST_ADVANCEMENTS_IN_TEAM("bac_advfirst_team", isTeamsMode = true);

    companion object {
        fun fromObjective(name: String): BacapMode? =
            entries.find { it.objectiveName == name }
    }
}
