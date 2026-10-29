package io.github.diskria.achievetodo.server;

import io.github.diskria.achievetodo.AchieveToDoMod;

public class Constants {

    public static final int TOTAL_ADVANCEMENTS_COUNT = 1152;

    public static class Progression {
        public static final int MIN_ADVANCEMENTS_COUNT = 2;

        public static final int INITIALLY_UNLOCKED_FLAG = 0;
        public static final int PERMANENTLY_LOCKED_FLAG = -1;
    }

    public static class NbtKey {
        public static final String LEVEL_CONFIG_NAME = AchieveToDoMod.ID + "_" + "configName";
    }

    public static class ConfigKey {
        public static final String SERVER_CONFIG_PROPERTY_NAME = AchieveToDoMod.ID + "-" + "configName";

        public static final String VERSION = "version";
        public static final String ABILITIES_TABLE = "abilities";
    }

    public static class FileExtension {
        public static final String TOML = ".toml";
        public static final String ZIP = ".zip";
    }
}
