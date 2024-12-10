package de.canitzp.solarhelmet;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.internal.NeoForgeRecipeProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = SolarHelmet.MODID)
public class SolarHelmetData {

    @SubscribeEvent
    public static void runData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ItemModel(generator.getPackOutput(), helper));
        event.createProvider(event.includeServer(), Recipe.Runner::new);
    }

    public static class ItemModel extends ItemModelProvider {

        public ItemModel(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, SolarHelmet.MODID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            this.singleTexture(SolarHelmet.SOLAR_MODULE_ITEM.get());
        }

        private void singleTexture(Item item){
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            singleTexture(key.getPath(), mcLoc("item/handheld"), "layer0", modLoc("item/" + key.getPath()));
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
