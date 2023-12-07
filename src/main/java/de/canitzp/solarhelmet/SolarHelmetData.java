package de.canitzp.solarhelmet;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = SolarHelmet.MODID)
public class SolarHelmetData {

    @SubscribeEvent
    public static void runData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new ItemModel(generator.getPackOutput(), helper));
        generator.addProvider(event.includeServer(), new Recipe(generator.getPackOutput(), event.getLookupProvider()));
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

        public Recipe(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected void buildRecipes(@NotNull RecipeOutput output) {
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SolarHelmet.SOLAR_MODULE_ITEM.get())
                    .define('r', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                    .define('l', Tags.Items.GEMS_LAPIS)
                    .define('d', Tags.Items.GEMS_DIAMOND)
                    .define('i', Tags.Items.INGOTS_IRON)
                    .define('p', Items.PAPER)
                    .pattern(" r ")
                    .pattern("ldl")
                    .pattern("ipi")
                    .unlockedBy("has_diamond", has(Tags.Items.GEMS_DIAMOND))
                    .save(output);
        }
    }

}
