package de.canitzp.solarhelmet;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author canitzp
 */
public class ItemSolarModule extends Item {

    public ItemSolarModule(){
        super(new Properties().group(SolarHelmet.TAB).maxStackSize(1));
        this.setRegistryName(SolarHelmet.MODID, "solar_helmet_module");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("item.solarhelmet:solar_helmet_module.desc").setStyle(new Style().setColor(TextFormatting.GRAY)));
    }
}
