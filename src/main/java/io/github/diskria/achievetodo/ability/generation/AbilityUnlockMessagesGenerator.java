package io.github.diskria.achievetodo.ability.generation;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import io.github.diskria.achievetodo.AchieveToDoMod;
import io.github.diskria.achievetodo.ability.AbilityType;
import io.github.diskria.achievetodo.client.gui.DesignCodePalette;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class AbilityUnlockMessagesGenerator implements DataProvider {

    protected final FabricDataOutput dataOutput;

    protected AbilityUnlockMessagesGenerator(FabricDataOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        return CompletableFuture.runAsync(() -> {
            try {
                createFunctions(writer);
            } catch (IOException e) {
                AchieveToDoMod.logger.error("Error while generating AbilityUnlockMessages:", e);
            }
        }, Util.getMainWorkerExecutor());
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    private void createFunctions(DataWriter dataWriter) throws IOException {
        for (AbilityType abilityType : AbilityType.values()) {
            String function = buildFunction(abilityType);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            HashingOutputStream hashingOutputStream = new HashingOutputStream(Hashing.sha1(), byteArrayOutputStream);
            try (BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(hashingOutputStream, StandardCharsets.UTF_8))
            ) {
                bufferedWriter.write(function);
            }
            Path functionsPath = dataOutput
                .getResolver(DataOutput.OutputType.DATA_PACK, "function")
                .resolve(AbilityAdvancementsGenerator.buildAdvancementId(abilityType), "mcfunction");
            dataWriter.write(functionsPath, byteArrayOutputStream.toByteArray(), hashingOutputStream.hash());
        }
    }

    private @NotNull String buildFunction(@NotNull AbilityType ability) {
        String function = """
            tellraw @a {
                "translate":"{MOD_ID}.ability_unlocked_chat_message",
                "with":[
                    {
                        "selector":"@s"
                    },
                    {
                        "color":"{COLOR}",
                        "text":"["
                    },
                    {
                        "color":"{COLOR}",
                        "translate":"{MOD_ID}.ability.{NAME}.name",
                        "clickEvent":{
                            "action":"run_command",
                            "value":"/advancementssearch highlight {ADVANCEMENT_ID} obtained_status"
                        },
                        "hoverEvent":{
                            "action":"show_text",
                            "contents":{
                                "color":"{COLOR}",
                                "translate":"{MOD_ID}.ability.{NAME}.name",
                                "extra":[
                                    {
                                        "text":"\\n"
                                    },
                                    {
                                        "color":"{COLOR}",
                                        "translate":"{MOD_ID}.ability.{NAME}.description"
                                    },
                                    {
                                        "text":"\\n\\n"
                                    },
                                    {
                                        "color":"gray",
                                        "italic":true,
                                        "translate":"%1$s tab",
                                        "with":[
                                            {
                                                "text":"{MOD_NAME}"
                                            }
                                        ]
                                    }
                                ]
                            }
                        }
                    },
                    {
                        "color":"{COLOR}",
                        "text":"]"
                    }
                ]
            }
            """
            .replace("{NAME}", ability.getName())
            .replace("{ADVANCEMENT_ID}", AbilityAdvancementsGenerator.buildAdvancementId(ability).toString())
            .replace("{COLOR}", DesignCodePalette.TEXT_COLOR_NAME)
            .replace("{MOD_NAME}", AchieveToDoMod.NAME)
            .replace("{MOD_ID}", AchieveToDoMod.ID);
        return String.join("", Arrays.stream(function.split("\\R")).map(String::trim).toArray(String[]::new));
    }
}
