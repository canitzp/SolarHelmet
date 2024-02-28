package de.canitzp.solarhelmet;

import de.canitzp.solarhelmet.recipe.RecipeModuleAddition;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.IPlantable;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author canitzp
 */
@Mod(SolarHelmet.MODID)
public class SolarHelmet{
    
    public static final String MODID = "solarhelmet";
    
    private static final Logger LOGGER = LogManager.getLogger(SolarHelmet.MODID);

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final Holder<CreativeModeTab> TAB = TABS.register("tab", SolarHelmetTab::create);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZER = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final Supplier<RecipeSerializer<RecipeModuleAddition>> MODULE_ADDITION_SERIALIZER = RECIPE_SERIALIZER.register("module_addition", RecipeModuleAddition.Serializer::new);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final Supplier<ItemSolarModule> SOLAR_MODULE_ITEM = ITEMS.register("solar_helmet_module", ItemSolarModule::new);
    
    public SolarHelmet(){
        LOGGER.info("Solar Helmet loading...");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SolarHelmetConfig.spec);

        IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();

        TABS.register(bus);
        RECIPE_SERIALIZER.register(bus);
        ITEMS.register(bus);
        LOGGER.info("Solar Helmet loaded.");
    }
    
    @Mod.EventBusSubscriber
    public static class ForgeEvents{
    
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void renderTooltips(ItemTooltipEvent event){
            if(!event.getItemStack().isEmpty()){
                CompoundTag nbt = event.getItemStack().getTag();
                if(nbt != null && nbt.contains("SolarHelmet", Tag.TAG_BYTE)){
                    event.getToolTip().add(Component.translatable("item.solarhelmet:solar_helmet_module_installed.text").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                    if(SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get() > 0){
                        event.getToolTip().add(Component.translatable("item.solarhelmet:solar_helmet_energy.text", nbt.getInt("solar_helmet_energy_stored"), SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()).withStyle(ChatFormatting.RED));
                    }
                }
            }
        }
    
        @SubscribeEvent
        public static void resourceReload(AddReloadListenerEvent event){
            event.addListener(new SimplePreparableReloadListener<RecipeManager>() {
                @Override
                protected RecipeManager prepare(ResourceManager iResourceManager, ProfilerFiller iProfiler){
                    return event.getServerResources().getRecipeManager();
                }
            
                @Override
                protected void apply(RecipeManager recipeManager, ResourceManager iResourceManager, ProfilerFiller iProfiler){
                    createHelmetRecipes(recipeManager);
                }
            });
        }
    
        private static void createHelmetRecipes(RecipeManager recipeManager){
            LOGGER.info("Solar Helmet recipe injecting...");
    
            // list which the old recipes are replaced with. This should include all existing recipes and the new ones, before recipeManager#replaceRecipes is called!
            List<RecipeHolder<?>> allNewRecipes = new ArrayList<>();
            for(Item helmet : BuiltInRegistries.ITEM){
                if(SolarHelmet.isItemHelmet(helmet)){
                    ResourceLocation helmetKey = BuiltInRegistries.ITEM.getKey(helmet);
                    // create recipe id for creation recipe
                    ResourceLocation craftingIdCreation = new ResourceLocation(MODID, "solar_helmet_creation_" + helmetKey.getNamespace() + "_" + helmetKey.getPath());
                    // create recipe id for removal recipe
                    ResourceLocation craftingIdRemoval = new ResourceLocation(MODID, "solar_helmet_removal_" + helmetKey.getNamespace() + "_" + helmetKey.getPath());
                    // create recipe for creation
                    Recipe<?> creationRecipe = SolarRecipeManager.creationRecipe(helmet);
                    // create recipe for removal
                    Recipe<?> removalRecipe = SolarRecipeManager.removalRecipe(helmet);

                    // add creation recipe to recipes list
                    if(recipeManager.getRecipeIds().noneMatch(resourceLocation -> resourceLocation.equals(craftingIdCreation))){
                        allNewRecipes.add(new RecipeHolder<>(craftingIdCreation, creationRecipe));
                        LOGGER.info(String.format("Solar Helmet created recipe for %s with id '%s'", helmetKey.toString(), craftingIdCreation));
                    }
                    // add removal recipe to recipes list
                    if(recipeManager.getRecipeIds().noneMatch(resourceLocation -> resourceLocation.equals(craftingIdRemoval))){
                        allNewRecipes.add(new RecipeHolder<>(craftingIdRemoval, removalRecipe));
                        LOGGER.info(String.format("Solar Helmet created recipe for %s with id '%s'", helmetKey.toString(), craftingIdRemoval));
                    }
                }
            }
            try{
                // add all existing recipes, since we're gonna replace them
                allNewRecipes.addAll(recipeManager.getRecipes());
                recipeManager.replaceRecipes(allNewRecipes);
            } catch(IllegalStateException e){
                LOGGER.error("Solar Helmet: Illegal recipe replacement caught! Report this to author immediately!", e);
            }
        }
    
        @SubscribeEvent
        public static void updatePlayer(TickEvent.PlayerTickEvent event){
            if(event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()){
                Inventory inv = event.player.getInventory();
                ItemStack helmet = inv.armor.get(EquipmentSlot.HEAD.getIndex());
                if(!helmet.isEmpty() && helmet.hasTag() && isItemHelmet(helmet.getItem())){
                    CompoundTag nbt = helmet.getTag();
                    if(nbt.contains("SolarHelmet",Tag.TAG_BYTE)){
                        if(isInRightDimension(event.player)){ // Produce energy
                            float energyMultiplierBasedOnSunlight = calculateSolarEnergy(event.player.level());
                            float energyMultiplierBasedOnAboveBlocks = calculateBlockingBlockPenalty(event.player);
                            float energyMultiplierFromConfig = SolarHelmetConfig.GENERAL.ENERGY_PRODUCTION_MULTIPLIER.get();

                            int producedEnergy = Math.round(SolarHelmetConfig.GENERAL.ENERGY_BASE_VALUE.get() * energyMultiplierBasedOnSunlight * energyMultiplierBasedOnAboveBlocks * energyMultiplierFromConfig);

                            int energyStored = nbt.getInt("solar_helmet_energy_stored");
                            if(energyStored < SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()){
                                int energyToStore = energyStored + producedEnergy;
                                if(energyToStore > SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()){
                                    nbt.putInt("solar_helmet_energy_stored", SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get());
                                }else{
                                    nbt.putInt("solar_helmet_energy_stored", energyToStore);
                                }
                            }
                        }
    
                        if(nbt.contains("solar_helmet_energy_stored", Tag.TAG_INT)){ // Consume energy
                            int storedEnergy = nbt.getInt("solar_helmet_energy_stored");
                            if(storedEnergy > 0){
                                AtomicInteger energyLeft = new AtomicInteger(storedEnergy);
                                for(ItemStack stack : getInventory(event.player)){ // Check if a item can be recharged
                                    IEnergyStorage capability = stack.getCapability(Capabilities.EnergyStorage.ITEM);
                                    if(capability != null){
                                        energyLeft.set(energyLeft.get() - capability.receiveEnergy(energyLeft.get(), false));
                                    }
                                    if(energyLeft.get() <= 0){
                                        break;
                                    }
                                }
                                nbt.putInt("solar_helmet_energy_stored", energyLeft.get());
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static boolean isItemHelmet(Item item){
        if(item instanceof ArmorItem && ((ArmorItem) item).getType().getSlot() == EquipmentSlot.HEAD){
            return !SolarHelmetConfig.GENERAL.HELMET_BLACKLIST.get().contains(BuiltInRegistries.ITEM.getKey(item).toString());
        }
        return SolarHelmetConfig.GENERAL.HELMET_WHITELIST.get().contains(BuiltInRegistries.ITEM.getKey(item).toString());
    }
    
    private static boolean isInRightDimension(Player player){
        return !SolarHelmetConfig.GENERAL.DIMENSION_BLACKLIST.get().contains(player.level().dimension().location().toString());
    }

    /**
     * Plots the current daytime on a graph with 1 as upper and -0.8 as lower bound.
     * Values under 0 are returned as 0.
     * The graph itself is only 0.9 tall, but is shifted up by 0.1, so that the night is 2 hours shorter than the day.
     * This should be used as multiplier for energy production.
     * @param level Level to calculate in.
     * @return
     */
    private static float calculateSolarEnergy(Level level){
        long daytime = level.getDayTime() % 24000L;
        return Math.max(0F, 0.9F * (float) Math.sin((Math.PI / 12000L) * daytime) + 0.1F);
    }

    /**
     * Calculates how much energy penalties are given, based on all blocks above the player.
     *
     * @param player
     * @return Float where 1F is no penalty and 0F would be full-blocking (stops energy production).
     */
    private static float calculateBlockingBlockPenalty(Player player){
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(player.getBlockX(), player.getBlockY() + 1, player.getBlockZ());
        float multiplier = 1F;
        for(int y = pos.getY(); y < player.level().getHeight(); y++){
            pos.setY(y);
            multiplier *= calculateDirectBlockOpaqueMultiplier(player.level(), pos);
            if(multiplier <= 0F){
                break;
            }
        }
        return multiplier;
    }

    /**
     * Calculate the multiplier (penalty) of the block at the given position.
     *
     * @param level
     * @param pos
     * @return Float where 1F is no penalty and 0F would be full-blocking (stops energy production).
     */
    private static float calculateDirectBlockOpaqueMultiplier(Level level, BlockPos pos){
        BlockState state = level.getBlockState(pos);

        if(SolarHelmetConfig.GENERAL.ADDITIONAL_OPAQUE_BLOCKS.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())){
            return 0F;
        }
        if(SolarHelmetConfig.GENERAL.ADDITIONAL_PARTLY_OPAQUE_BLOCKS.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())){
            return 1F;
        }
        if(SolarHelmetConfig.GENERAL.ADDITIONAL_NON_OPAQUE_BLOCKS.get().contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())){
            return 0F;
        }
        if(state.getBlock() instanceof IPlantable){
            return 1F;
        }
        
        return state.getShadeBrightness(level, pos);
    }

    /**
     * Custom implementation for checking if it is day.
     * Minecraft days are a time span from 0 to 24000, but all values above are also valid and represent the days passed.
     * We can get rid of the day counter if we mod the daytime by 24000 and so get the actual time of the day.
     * Time map:
     *  0: 6am
     *  6000: 12pm
     *  12000: 6pm
     *  18000: 12am (midnight)
     *  Therefor this formula assumes everyday starts at 6am (0) and ends at 8pm (14000).
     * @param level The world where the check should be performed.
     * @return If it is day or not. (Day is from 6am to 8pm)
     */
    private static boolean isDaytime(Level level){
        long daytime = level.getDayTime() % 24000L;
        return daytime < 14000;
    }
    
    // copy solar helmet tag and energy stored to new stack
    @SubscribeEvent
    public static void anvilRepair(AnvilRepairEvent event){
        ItemStack inputStack = event.getLeft();
        ItemStack resultStack = event.getOutput();
        
        if(inputStack.hasTag()){
            CompoundTag inputStackTag = inputStack.getTag();
            CompoundTag resultStackTag = resultStack.hasTag() ? resultStack.getTag() : new CompoundTag();
    
            if(inputStackTag.contains("SolarHelmet", Tag.TAG_BYTE)){
                resultStackTag.putBoolean("SolarHelmet", inputStackTag.getBoolean("SolarHelmet"));
            }
    
            if(inputStackTag.contains("solar_helmet_energy_stored", Tag.TAG_INT)){
                resultStackTag.putInt("solar_helmet_energy_stored", inputStackTag.getInt("solar_helmet_energy_stored"));
            }
            
            resultStack.setTag(resultStackTag);
        }
        
    }
    
    public static NonNullList<ItemStack> getInventory(Player player){
        NonNullList<ItemStack> list = NonNullList.create();
        list.addAll(player.getInventory().items);
        list.addAll(player.getInventory().armor);
        list.addAll(player.getInventory().offhand);
        return list;
    }
    
}