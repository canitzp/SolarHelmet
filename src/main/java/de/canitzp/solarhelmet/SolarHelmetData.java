package de.canitzp.solarhelmet;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = SolarHelmet.MODID)
public class SolarHelmetData {

    @SubscribeEvent
    public static void runData(GatherDataEvent.Client event){
        event.createProvider(ItemModel::new);
        event.createProvider(Recipe.Runner::new);
    }

    public static class ItemModel extends ModelProvider {

        public ItemModel(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, SolarHelmet.MODID);
        }

        @Override
        protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
            itemModels.generateFlatItem(SolarHelmet.SOLAR_MODULE_ITEM.get(), ModelTemplates.FLAT_ITEM);
        }
    }

    public static class Recipe extends RecipeProvider {

        public Recipe(HolderLookup.Provider provider, RecipeOutput output) {
            super(provider, output);
        }

        @Override
        protected void buildRecipes() {
            ShapedRecipeBuilder.shaped(BuiltInRegistries.ITEM, RecipeCategory.TOOLS, SolarHelmet.SOLAR_MODULE_ITEM.get())
                    .define('r', tag(Tags.Items.STORAGE_BLOCKS_REDSTONE))
                    .define('l', tag(Tags.Items.GEMS_LAPIS))
                    .define('d', tag(Tags.Items.GEMS_DIAMOND))
                    .define('i', tag(Tags.Items.INGOTS_IRON))
                    .define('p', Items.PAPER)
                    .pattern(" r ")
                    .pattern("ldl")
                    .pattern("ipi")
                    .unlockedBy("has_diamond", has(Tags.Items.GEMS_DIAMOND))
                    .save(super.output);
        }

        public static final class Runner extends RecipeProvider.Runner {
            public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
                super(output, lookupProvider);
            }

            @Override
            protected RecipeProvider createRecipeProvider(HolderLookup.Provider lookupProvider, RecipeOutput output) {
                return new Recipe(lookupProvider, output);
            }

            @Override
            public String getName() {
                return "SolarHelmet recipes";
            }
        }
    }

}
