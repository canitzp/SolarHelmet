package de.canitzp.solarhelmet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = SolarHelmet.MODID)
public class SolarHelmetTab {

    @SubscribeEvent
    public static void registerCreativeTab(CreativeModeTabEvent.Register event){
        event.registerCreativeModeTab(new ResourceLocation(SolarHelmet.MODID, "tab"), builder -> {
            builder.icon(() -> SolarHelmet.SOLAR_MODULE_ITEM.get().getDefaultInstance());
            builder.title(Component.translatable("tab.solarhelmet"));
            builder.displayItems((parameters, output) -> {
                output.accept(SolarHelmet.SOLAR_MODULE_ITEM.get().getDefaultInstance());
                for(Item item : ForgeRegistries.ITEMS){
                    if(SolarHelmet.isItemHelmet(item)){
                        ItemStack stack = new ItemStack(item);
                        CompoundTag tag = new CompoundTag();
                        tag.putBoolean("SolarHelmet", true);
                        stack.setTag(tag);
                        output.accept(stack);
                    }
                }
            });
        });
    }
}
