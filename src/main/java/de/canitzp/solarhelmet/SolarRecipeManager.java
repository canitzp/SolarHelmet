package de.canitzp.solarhelmet;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SolarRecipeManager {

    public static Recipe<?> creationRecipe(final Item helmet, final ResourceLocation craftingId){
        ItemStack outputStack = helmet.getDefaultInstance();
        outputStack.getOrCreateTag().putBoolean("SolarHelmet", true);

        return new SmithingRecipe() {
            @Override
            public boolean isTemplateIngredient(ItemStack stack) {
                return stack.isEmpty();
            }

            @Override
            public boolean isBaseIngredient(ItemStack stack) {
                return stack.is(helmet) && !(stack.hasTag() && stack.getTag().getBoolean("SolarHelmet"));
            }

            @Override
            public boolean isAdditionIngredient(ItemStack stack) {
                return stack.is(SolarHelmet.SOLAR_MODULE_ITEM.get());
            }

            @Override
            public boolean matches(Container container, Level level) {
                return isTemplateIngredient(container.getItem(0)) && isBaseIngredient(container.getItem(1)) && isAdditionIngredient(container.getItem(2));
            }

            @Override
            public ItemStack assemble(Container container, RegistryAccess access) {
                ItemStack assembled = this.getResultItem(access).copy();
                // copy old nbt to new stack
                assembled.getOrCreateTag().merge(container.getItem(1).getOrCreateTag());
                // set solar helmet flag
                assembled.getOrCreateTag().putBoolean("SolarHelmet", true);
                return assembled;
            }

            @Override
            public ItemStack getResultItem(RegistryAccess access) {
                return outputStack;
            }

            @Override
            public ResourceLocation getId() {
                return craftingId;
            }

            @Override
            public RecipeSerializer<?> getSerializer() {
                return RecipeSerializer.SMITHING_TRANSFORM;
            }
        };
    }

    public static Recipe<?> removalRecipe(final Item helmet, final ResourceLocation craftingId){
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(helmet));
        ItemStack outputStack = helmet.getDefaultInstance();
        return new ShapelessRecipe(craftingId, "", CraftingBookCategory.EQUIPMENT, outputStack, ingredients){
            // copy nbt tag from helmet to new helmet, also delete SolarHelmet tag
            @Override
            public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
                ItemStack assembled = super.assemble(container, access);
                ItemStack inputStack = ItemStack.EMPTY;
                for (int slotId = 0; slotId < container.getContainerSize(); slotId++) {
                    if(!container.getItem(slotId).isEmpty()){
                        inputStack = container.getItem(slotId).copy();
                        break;
                    }
                }
                if(!inputStack.isEmpty()){
                    if(inputStack.hasTag()){
                        CompoundTag inputTag = inputStack.getTag();
                        inputTag.remove("SolarHelmet");
                        assembled.setTag(inputTag);
                    }
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
                if (!inputStack.hasTag()) {
                    return false;
                }
                return inputStack.getTag().getBoolean("SolarHelmet");
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
