package de.canitzp.solarhelmet;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = SolarHelmet.MODID)
public class SolarHelmetData {

    @SubscribeEvent
    public static void runData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new ItemModel(generator.getPackOutput(), helper));
        generator.addProvider(event.includeServer(), new Recipe(generator.getPackOutput()));
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
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
            singleTexture(key.getPath(), mcLoc("item/handheld"), "layer0", modLoc("item/" + key.getPath()));
        }
    }

    public static class Recipe extends RecipeProvider {

        public Recipe(PackOutput output) {
            super(output);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
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
                    .save(consumer);
        }
    }

}
