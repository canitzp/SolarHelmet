package de.canitzp.solarhelmet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class SolarHelmetTab {

    public static CreativeModeTab create() {
        return CreativeModeTab.builder()
                .icon(() -> SolarHelmet.SOLAR_MODULE_ITEM.get().getDefaultInstance())
                .title(Component.translatable("tab.solarhelmet"))
                .displayItems((parameters, output) -> {
                    output.accept(SolarHelmet.SOLAR_MODULE_ITEM.get().getDefaultInstance());
                    for (Item item : ForgeRegistries.ITEMS) {
                        if (SolarHelmet.isItemHelmet(item)) {
                            ItemStack stack = new ItemStack(item);
                            CompoundTag tag = new CompoundTag();
                            tag.putBoolean("SolarHelmet", true);
                            stack.setTag(tag);
                            output.accept(stack);
                        }
                    }
                }).build();
    }
}
