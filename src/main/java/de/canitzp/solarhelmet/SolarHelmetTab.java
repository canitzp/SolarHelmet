package de.canitzp.solarhelmet;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SolarHelmetTab {

    public static CreativeModeTab create() {
        return CreativeModeTab.builder()
                .icon(() -> SolarHelmet.SOLAR_MODULE_ITEM.get().getDefaultInstance())
                .title(Component.translatable("tab.solarhelmet"))
                .displayItems((parameters, output) -> {
                    output.accept(SolarHelmet.SOLAR_MODULE_ITEM.get().getDefaultInstance());
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (SolarHelmet.isItemHelmet(item)) {
                            ItemStack stack = item.getDefaultInstance();
                            SolarHelmet.enableSolarHelmet(stack);
                            output.accept(stack);
                        }
                    }
                }).build();
    }
}
