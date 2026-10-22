package io.github.diskria.achievetodo.ability;

import io.github.diskria.achievetodo.server.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChaosProgressionGenerator {

    private static final int MAX_ADVANCEMENTS_COUNT = 1000;

    private static final double CHAOS_INITIALLY_UNLOCKED_CHANCE_PERCENT = 1.0;
    private static final double CHAOS_PERMANENTLY_LOCKED_CHANCE_PERCENT = 0.2;

    private final @NotNull Map<AbilityType, Integer> baseProgression;
    private final Random random;
    private @NotNull Map<AbilityType, Integer> progression;

    private ChaosProgressionGenerator(@NotNull Map<AbilityType, Integer> baseProgression, Random random) {
        this.baseProgression = baseProgression;
        this.progression = Map.copyOf(baseProgression);
        this.random = random;
    }

    public static @NotNull Map<AbilityType, Integer> generateChaosProgression(long seed) {
        ChaosProgressionGenerator generator = new ChaosProgressionGenerator(
            Progressions.getHardProgression(),
            new Random(seed)
        );
        generator
            .shuffle()
            .shift()
            .fixPriority()
            .applySpecialFlags();
        return generator.progression;
    }

    private ChaosProgressionGenerator shuffle() {
        List<AbilityType> abilityTypes = new ArrayList<>(progression.keySet());
        List<Integer> counts = new ArrayList<>(progression.values());
        Collections.shuffle(abilityTypes, random);
        Collections.shuffle(counts, random);
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < abilityTypes.size(); i++) {
            result.put(abilityTypes.get(i), counts.get(i));
        }
        progression = result;
        return this;
    }

    private ChaosProgressionGenerator shift() {
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        for (var entry : progression.entrySet()) {
            AbilityType abilityType = entry.getKey();
            int oldCount = entry.getValue();
            int priority = abilityType.getChaosPriority();
            int newCount;
            if (priority == 100) {
                newCount = oldCount;
            } else {
                int shiftRange = Math.round((100 - priority) * 0.5f);
                if (shiftRange < 0) {
                    shiftRange = 0;
                }
                int shiftMagnitude = random.nextInt(shiftRange);
                int shift = random.nextBoolean() ? shiftMagnitude : -shiftMagnitude;

                newCount = oldCount + shift;
            }
            if (newCount < Constants.Progression.MIN_ADVANCEMENTS_COUNT) {
                newCount = Constants.Progression.MIN_ADVANCEMENTS_COUNT;
            } else if (newCount > MAX_ADVANCEMENTS_COUNT) {
                newCount = MAX_ADVANCEMENTS_COUNT;
            }
            result.put(abilityType, newCount);
        }
        progression = result;
        return this;
    }

    private ChaosProgressionGenerator fixPriority() {
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<AbilityType, Integer> entry : progression.entrySet()) {
            AbilityType abilityType = entry.getKey();
            int oldCount = entry.getValue();
            int baseCount = baseProgression.get(abilityType);
            int priority = abilityType.getChaosPriority();
            final int newCount;
            if (priority == 0) {
                newCount = oldCount;
            } else if (priority == 100) {
                newCount = baseCount;
            } else {
                double ratio = priority / 100.0 * random.nextDouble();
                newCount = (int) Math.round(oldCount + ratio * (baseCount - oldCount));
            }
            result.put(abilityType, newCount);
        }
        progression = result;
        return this;
    }

    private void applySpecialFlags() {
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        for (var entry : progression.entrySet()) {
            AbilityType abilityType = entry.getKey();
            int oldCount = entry.getValue();
            int newCount;
            if (abilityType == AbilityType.VISION ||
                random.nextDouble() * 100.0 < CHAOS_INITIALLY_UNLOCKED_CHANCE_PERCENT
            ) {
                newCount = Constants.Progression.INITIALLY_UNLOCKED_FLAG;
            } else if (abilityType.getChaosPriority() != 100 &&
                random.nextDouble() * 100.0 < CHAOS_PERMANENTLY_LOCKED_CHANCE_PERCENT
            ) {
                newCount = Constants.Progression.PERMANENTLY_LOCKED_FLAG;
            } else {
                newCount = oldCount;
            }
            result.put(entry.getKey(), newCount);
        }
        progression = result;
    }
}
