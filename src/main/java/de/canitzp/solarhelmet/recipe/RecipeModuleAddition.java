package de.canitzp.solarhelmet.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.canitzp.solarhelmet.SolarHelmet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record RecipeModuleAddition(Item helmet, ItemStack result) implements SmithingRecipe {

    public RecipeModuleAddition(ResourceLocation loc, ItemStack outputStack) {
        this(BuiltInRegistries.ITEM.get(loc), outputStack);
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return stack.is(this.helmet) && !SolarHelmet.isSolarHelmet(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(SolarHelmet.SOLAR_MODULE_ITEM.get());
    }

    @Override
    public boolean matches(SmithingRecipeInput container, Level level) {
        return this.isTemplateIngredient(container.getItem(0)) && this.isBaseIngredient(container.getItem(1)) && this.isAdditionIngredient(container.getItem(2));
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput container, HolderLookup.Provider access) {
        ItemStack assembled = this.getResultItem(access).copy();
        // copy all components
        assembled.applyComponents(container.getItem(1).getComponents());
        // set solar helmet flag
        SolarHelmet.enableSolarHelmet(assembled);
        return assembled;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider access) {
        return this.result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<RecipeModuleAddition> {

        public static final Serializer INSTANCE = new Serializer();
        private static final MapCodec<RecipeModuleAddition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("helmet").forGetter(recipe -> BuiltInRegistries.ITEM.getKey(recipe.helmet)),
                ItemStack.CODEC.fieldOf("result").forGetter(RecipeModuleAddition::result)
        ).apply(instance, RecipeModuleAddition::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecipeModuleAddition> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork
        );

        public static @NotNull RecipeModuleAddition fromNetwork(RegistryFriendlyByteBuf buffer) {
            Item helmet = BuiltInRegistries.ITEM.get(buffer.readResourceLocation());
            ItemStack outputStack = ItemStack.STREAM_CODEC.decode(buffer);
            return new RecipeModuleAddition(helmet, outputStack);
        }

        @Override
        public @NotNull MapCodec<RecipeModuleAddition> codec(){
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RecipeModuleAddition> streamCodec() {
            return STREAM_CODEC;
        }

        public static void toNetwork(RegistryFriendlyByteBuf buffer, RecipeModuleAddition recipe) {
            buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(recipe.helmet));
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    }

}
