package de.canitzp.solarhelmet;

import java.util.function.Supplier;

public class SolarHelmetRegistry {

    public static Supplier<ItemSolarModule> SOLAR_MODULE_ITEM;

    public static void init(Platform platform){
        platform.registerItem("solar_helmet_module", ItemSolarModule::new);
    }

}
