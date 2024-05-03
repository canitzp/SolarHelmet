package de.canitzp.solarhelmet;

import de.canitzp.solarhelmet.recipe.RecipeModuleAddition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public class SolarRecipeManager {

    public static Recipe<?> creationRecipe(final Item helmet){
        ItemStack outputStack = helmet.getDefaultInstance();

        SolarHelmet.enableSolarHelmet(outputStack);

        return new RecipeModuleAddition(helmet, outputStack);
    }

    public static Recipe<?> removalRecipe(final Item helmet){
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(helmet));
        ItemStack outputStack = helmet.getDefaultInstance();
        return new ShapelessRecipe("", CraftingBookCategory.EQUIPMENT, outputStack, ingredients){
            // copy nbt tag from helmet to new helmet, also delete SolarHelmet tag
            @Override
            public ItemStack assemble(CraftingContainer container, HolderLookup.Provider access) {
                ItemStack assembled = super.assemble(container, access);
                ItemStack inputStack = ItemStack.EMPTY;
                for (int slotId = 0; slotId < container.getContainerSize(); slotId++) {
                    if(!container.getItem(slotId).isEmpty()){
                        inputStack = container.getItem(slotId).copy();
                        break;
                    }
                }
                if(!inputStack.isEmpty()){
                    if(SolarHelmet.isSolarHelmet(inputStack)){
                        inputStack.remove(SolarHelmet.DC_SOLAR_HELMET);
                    }
                    // Copy all components to assembled stack
                    assembled.applyComponents(inputStack.getComponents());
                }
                return assembled;
            }

            // only match if the input helmet has an enabled SolarHelmet module
            @Override
            public boolean matches(CraftingContainer container, Level level) {
                boolean matches = super.matches(container, level);
                if(!matches){
                    return false;
                }
                ItemStack inputStack = ItemStack.EMPTY;
                for (int slotId = 0; slotId < container.getContainerSize(); slotId++) {
                    if(!container.getItem(slotId).isEmpty()){
                        inputStack = container.getItem(slotId);
                        break;
                    }
                }
                if (inputStack.isEmpty()) {
                    return false; // this "should" never happen
                }
                return SolarHelmet.isSolarHelmet(inputStack);
            }

            @Override
            public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
                NonNullList<ItemStack> remainingItems = super.getRemainingItems(container);
                remainingItems.set(0, SolarHelmet.SOLAR_MODULE_ITEM.get().getDefaultInstance());
                return remainingItems;
            }
        };
    }

}
