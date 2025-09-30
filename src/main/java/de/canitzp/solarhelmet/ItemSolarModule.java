package de.canitzp.solarhelmet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author canitzp
 */
public class ItemSolarModule extends Item {

    public ItemSolarModule(ResourceLocation id) {
        super(new Properties().setId(ResourceKey.create(Registries.ITEM, id)).useItemDescriptionPrefix().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.solarhelmet:solar_helmet_module.desc").withStyle(ChatFormatting.GRAY));
    }
}
