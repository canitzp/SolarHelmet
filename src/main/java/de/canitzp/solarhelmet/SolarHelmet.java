package de.canitzp.solarhelmet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
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
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author canitzp
 */
@Mod(SolarHelmet.MODID)
public class SolarHelmet{
    
    public static final String MODID = "solarhelmet";
    
    private static final Logger LOGGER = LogManager.getLogger(SolarHelmet.MODID);
    
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<ItemSolarModule> SOLAR_MODULE_ITEM = ITEMS.register("solar_helmet_module", ItemSolarModule::new);
    
    public SolarHelmet(){
        LOGGER.info("Solar Helmet loading...");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SolarHelmetConfig.spec);
        
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
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
            List<Recipe<?>> allNewRecipes = new ArrayList<>();
            for(Item helmet : ForgeRegistries.ITEMS.getValues()){
                if(SolarHelmet.isItemHelmet(helmet)){
                    // create recipe input
                    NonNullList<Ingredient> recipeInput = NonNullList.create();
                    recipeInput.add(Ingredient.of(SOLAR_MODULE_ITEM.get()));
                    recipeInput.add(Ingredient.of(helmet));
                    List<String> add_craft_items = SolarHelmetConfig.GENERAL.ADD_CRAFT_ITEMS.get();
                    add_craft_items.stream()
                                   .limit(7)
                                   .map(s -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(s)))
                                   .filter(Objects::nonNull)
                                   .map(Ingredient::of)
                                   .forEach(recipeInput::add);
                    // create recipe output
                    ItemStack helmetStack = new ItemStack(helmet);
                    CompoundTag nbt = new CompoundTag();
                    nbt.putBoolean("SolarHelmet", true);
                    helmetStack.setTag(nbt);

                    ResourceLocation helmetKey = ForgeRegistries.ITEMS.getKey(helmet);
                    // create recipe id
                    ResourceLocation craftingId = new ResourceLocation(MODID, "solar_helmet_" + helmetKey.getNamespace() + "_" + helmetKey.getPath());
                    // create recipe
                    ShapelessRecipe recipe = new ShapelessRecipe(craftingId, "", CraftingBookCategory.EQUIPMENT, helmetStack, recipeInput) {
                        @Nonnull
                        @Override
                        public ItemStack assemble(CraftingContainer inv, RegistryAccess access){
                            CompoundTag nbt = new CompoundTag();
                            for(int i = 0; i < inv.getContainerSize(); i++){
                                ItemStack stack = inv.getItem(i);
                                if(!stack.isEmpty() && stack.getItem() instanceof ArmorItem){
                                    if(stack.hasTag()){
                                        nbt = stack.getTag().copy();
                                    }
                                }
                            }
                            ItemStack out = super.assemble(inv, access);
                            nbt.putBoolean("SolarHelmet", true);
                            out.setTag(nbt);
                            return out;
                        }
                
                        // checks if the helmet doesn't already have the module
                        @Override
                        public boolean matches(CraftingContainer inv, Level level1){
                            if(super.matches(inv, level1)){
                                for(int i = 0; i < inv.getContainerSize(); i++){
                                    ItemStack stack = inv.getItem(i);
                                    if(!stack.isEmpty() && stack.getItem() instanceof ArmorItem){
                                        CompoundTag nbt = stack.getTag();
                                        if(nbt != null && nbt.contains("SolarHelmet", Tag.TAG_BYTE)){
                                            return false;
                                        }
                                    }
                                }
                                return true;
                            }
                            return false;
                        }
                    };
            
                    if(recipeManager.getRecipeIds().noneMatch(resourceLocation -> resourceLocation.equals(craftingId))){
                        allNewRecipes.add(recipe);
                        LOGGER.info(String.format("Solar Helmet created recipe for %s with id '%s'", helmetKey.toString(), craftingId));
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
            if(event.phase == TickEvent.Phase.END && !event.player.level.isClientSide()){
                Inventory inv = event.player.getInventory();
                ItemStack helmet = inv.armor.get(EquipmentSlot.HEAD.getIndex());
                if(!helmet.isEmpty() && helmet.hasTag() && isItemHelmet(helmet.getItem())){
                    CompoundTag nbt = helmet.getTag();
                    if(nbt.contains("SolarHelmet",Tag.TAG_BYTE)){
                        if(isInRightDimension(event.player)){ // Produce energy
                            float energyMultiplierBasedOnSunlight = calculateSolarEnergy(event.player.level);
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
                                    stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energyStorage -> {
                                        energyLeft.set(energyLeft.get() - energyStorage.receiveEnergy(energyLeft.get(), false));
                                    });
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
            return !SolarHelmetConfig.GENERAL.HELMET_BLACKLIST.get().contains(ForgeRegistries.ITEMS.getKey(item).toString());
        }
        return SolarHelmetConfig.GENERAL.HELMET_WHITELIST.get().contains(ForgeRegistries.ITEMS.getKey(item).toString());
    }
    
    private static boolean isInRightDimension(Player player){
        return !SolarHelmetConfig.GENERAL.DIMENSION_BLACKLIST.get().contains(player.level.dimension().location().toString());
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
        for(int y = pos.getY(); y < player.level.getHeight(); y++){
            pos.setY(y);
            multiplier *= calculateDirectBlockOpaqueMultiplier(player.level, pos);
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

        if(SolarHelmetConfig.GENERAL.ADDITIONAL_OPAQUE_BLOCKS.get().contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString())){
            return 0F;
        }
        if(SolarHelmetConfig.GENERAL.ADDITIONAL_PARTLY_OPAQUE_BLOCKS.get().contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString())){
            return 1F;
        }
        if(SolarHelmetConfig.GENERAL.ADDITIONAL_NON_OPAQUE_BLOCKS.get().contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString())){
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