package de.canitzp.solarhelmet;

import com.google.common.collect.Lists;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class SolarHelmetConfig{
    
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final General GENERAL = new General(BUILDER);
    public static final ModConfigSpec spec = BUILDER.build();

    public static class General {
        public final ModConfigSpec.ConfigValue<Integer> ENERGY_BASE_VALUE;
        public final ModConfigSpec.ConfigValue<Float> ENERGY_PRODUCTION_MULTIPLIER;
        public final ModConfigSpec.ConfigValue<Double> ENERGY_PRODUCTION_NIGHT_MULTIPLIER;
        public final ModConfigSpec.ConfigValue<Double> ENERGY_PRODUCTION_RAIN_MULTIPLIER;
        public final ModConfigSpec.ConfigValue<Integer> ENERGY_STORAGE;
        public final ModConfigSpec.ConfigValue<List<String>> ADD_CRAFT_ITEMS;
        public final ModConfigSpec.ConfigValue<List<String>> HELMET_BLACKLIST;
        public final ModConfigSpec.ConfigValue<List<String>> HELMET_WHITELIST;
        public final ModConfigSpec.ConfigValue<List<String>> DIMENSION_BLACKLIST;
        public final ModConfigSpec.ConfigValue<List<String>> ADDITIONAL_OPAQUE_BLOCKS;
        public final ModConfigSpec.ConfigValue<List<String>> ADDITIONAL_PARTLY_OPAQUE_BLOCKS;
        public final ModConfigSpec.ConfigValue<List<String>> ADDITIONAL_NON_OPAQUE_BLOCKS;
    
        public General(ModConfigSpec.Builder builder) {
            builder.push("General");
            ENERGY_BASE_VALUE = builder
                .comment("Base energy production of the helmet.")
                .translation("Energy base value")
                .define("energy_base_value", 15);
            ENERGY_PRODUCTION_MULTIPLIER = builder
                .comment("How much energy should the helmet produce?",
                    "For this value equaling 1.0, the 'energy_base_value' is produces while in direct sunlight.",
                    "Any changes to this configuration are directly multiplied with the final energy production value, after any other calculation (sunlight strength and opaque blocks")
                .translation("Energy production multiplier")
                .define("energy_production_multiplier", 1.0F);
            ENERGY_PRODUCTION_NIGHT_MULTIPLIER = builder
                .comment("How much should the night reduce the energy value?",
                    "1.0 is equal to 100% energy, so there is no decrease at all.",
                    "0.5 is equal to 50% which would cut energy production in half while nighttime.",
                    "0 would stop producing energy while nighttime.",
                    "A value higher than 1.0 would cause more energy production while it's nighttime.")
                .translation("Energy production night multiplier")
                .defineInRange("energy_production_night_multiplier", 0D, 0D, 1000000D);
            ENERGY_PRODUCTION_RAIN_MULTIPLIER = builder
                .comment("How much should rain reduce the energy value?",
                    "1.0 is equal to 100% energy, so there is no decrease at all.",
                    "0.5 is equal to 50% which would cut energy production in half while raining.",
                    "0 would stop producing energy while raining.",
                    "A value higher than 1.0 would cause more energy production while it's raining.")
                .translation("Energy production rain multiplier")
                .defineInRange("energy_production_rain_multiplier", 0.5D, 0D, 1000000D);
            ENERGY_STORAGE = builder
                .comment("How much energy should the helmet be able to hold?",
                    "This is fully independent from the normal energy saving (for e.g a electric helmet from some kind of mod).")
                .translation("Energy production multiplier")
                .defineInRange("energy_storage", 20000, 0, Integer.MAX_VALUE);
            ADD_CRAFT_ITEMS = builder
                .comment("Put additional items to craft a helmet with a module in here. Up to 7")
                .translation("Additional crafting items")
                .worldRestart()
                .define("additional_crafting_items", new ArrayList<>());
            HELMET_BLACKLIST = builder
                .comment("The here stated items can't be used as Solar Helmet")
                .translation("Helmet blacklist")
                .worldRestart()
                .define("helmet_blacklist", new ArrayList<>());
            HELMET_WHITELIST = builder
                .comment("The here stated items can be used as Solar Helmet, even when they aren't helmets at all (You can't put everything in you helmet slot)")
                .translation("Helmet whitelist")
                .worldRestart()
                .define("helmet_whitelist", new ArrayList<>());
            DIMENSION_BLACKLIST = builder
                .comment("The dimensions in here are excluded from energy production.")
                .translation("Dimension blacklist")
                .worldRestart()
                .define("dimension_blacklist", Lists.newArrayList(Level.NETHER.location().toString(), Level.END.location().toString()));
            ADDITIONAL_OPAQUE_BLOCKS = builder
                .comment("Additional blocks where sun in shining completely through.")
                .translation("Additional opaque blocks")
                .worldRestart()
                .define("additional_opaque_blocks", new ArrayList<>());
            ADDITIONAL_PARTLY_OPAQUE_BLOCKS = builder
                .comment("Additional blocks where sun in shining partly through.")
                .translation("Additional partly opaque blocks")
                .worldRestart()
                .define("additional_partly_opaque_blocks", new ArrayList<>());
            ADDITIONAL_NON_OPAQUE_BLOCKS = builder
                .comment("Additional blocks where sun can't pass through.")
                .translation("Additional non opaque blocks")
                .worldRestart()
                .define("additional_non_opaque_blocks", new ArrayList<>());
            builder.pop();
        }
    }
    
}