package de.canitzp.solarhelmet;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SpecialPlantable;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);
    public static final Supplier<ItemSolarModule> SOLAR_MODULE_ITEM = ITEMS.register("solar_helmet_module", ItemSolarModule::new);
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPE = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final Supplier<DataComponentType<Boolean>> DC_SOLAR_HELMET = DATA_COMPONENT_TYPE.registerComponentType("solar_helmet", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).cacheEncoding());
    public static final Supplier<DataComponentType<Integer>> DC_SOLAR_HELMET_ENERGY = DATA_COMPONENT_TYPE.registerComponentType("solar_helmet_energy", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).cacheEncoding());

    public SolarHelmet(IEventBus modEventBus, ModContainer modContainer){
        LOGGER.info("Solar Helmet loading...");

        modContainer.registerConfig(ModConfig.Type.COMMON, SolarHelmetConfig.spec);

        DATA_COMPONENT_TYPE.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        RECIPE_SERIALIZER.register(modEventBus);

        LOGGER.info("Solar Helmet loaded.");
    }
    
    @EventBusSubscriber
    public static class ForgeEvents{

        @SubscribeEvent
        public static void playerJoin(PlayerEvent.PlayerLoggedInEvent event){
            NonNullList<ItemStack> mergedInventory = NonNullList.create();
            for (ItemStack itemStack : event.getEntity().getInventory()) {
                mergedInventory.add(itemStack);
            }

            for(ItemStack stack : mergedInventory){
                if(stack.has(DataComponents.CUSTOM_DATA)){
                    CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
                    // update from pre 1.20.6 versions
                    if(tag.contains("SolarHelmet")){
                        tag.remove("SolarHelmet");
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        stack.set(DC_SOLAR_HELMET, true);
                    }
                    if(tag.contains("solar_helmet_energy_stored")){
                        int energy = tag.getIntOr("solar_helmet_energy_stored", 0);
                        tag.remove("solar_helmet_energy_stored");
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        stack.set(DC_SOLAR_HELMET_ENERGY, energy);
                    }
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void renderTooltips(ItemTooltipEvent event){
            if(!event.getItemStack().isEmpty()){
                if(isSolarHelmet(event.getItemStack())){
                    event.getToolTip().add(Component.translatable("item.solarhelmet:solar_helmet_module_installed.text").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                    if(SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get() > 0){
                        event.getToolTip().add(Component.translatable("item.solarhelmet:solar_helmet_energy.text", event.getItemStack().getOrDefault(DC_SOLAR_HELMET_ENERGY, 0), SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()).withStyle(ChatFormatting.RED));
                    }
                }
            }
        }
    
        @SubscribeEvent
        public static void updatePlayer(PlayerTickEvent.Post event){
            if(!event.getEntity().level().isClientSide()){
                ItemStack helmet = getHelmet(event.getEntity());
                if(!helmet.isEmpty() && isItemHelmet(helmet)){
                    if(isSolarHelmet(helmet)) {
                        int storedEnergy = helmet.getOrDefault(DC_SOLAR_HELMET_ENERGY, 0);
                        if (isInRightDimension(event.getEntity())) { // Produce energy
                            float energyMultiplierBasedOnSunlight = calculateSolarEnergy(event.getEntity().level());
                            float energyMultiplierBasedOnAboveBlocks = calculateBlockingBlockPenalty(event.getEntity());
                            float energyMultiplierFromConfig = SolarHelmetConfig.GENERAL.ENERGY_PRODUCTION_MULTIPLIER.get();

                            int producedEnergy = Math.round(SolarHelmetConfig.GENERAL.ENERGY_BASE_VALUE.get() * energyMultiplierBasedOnSunlight * energyMultiplierBasedOnAboveBlocks * energyMultiplierFromConfig);

                            if (storedEnergy < SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()) {
                                int energyToStore = Math.min(storedEnergy + producedEnergy, SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()); // cap at configured limit
                                storedEnergy = energyToStore;
                                helmet.set(DC_SOLAR_HELMET_ENERGY, energyToStore);
                            }
                        }

                        // Consume energy
                        if (storedEnergy > 0) {
                            AtomicInteger energyLeft = new AtomicInteger(storedEnergy);
                            for (ItemStack stack : getInventory(event.getEntity())) { // Check if a item can be recharged
                                IEnergyStorage capability = stack.getCapability(Capabilities.EnergyStorage.ITEM);
                                if (capability != null) {
                                    energyLeft.set(energyLeft.get() - capability.receiveEnergy(energyLeft.get(), false));
                                }
                                if (energyLeft.get() <= 0) {
                                    break;
                                }
                            }
                            helmet.set(DC_SOLAR_HELMET_ENERGY, energyLeft.get());
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerRightClicks(PlayerInteractEvent.RightClickItem event) {
            ItemStack heldStack = event.getItemStack();
            if (isItemHelmet(heldStack)) {
                // remove all modules from helmet
                if (event.getEntity().isShiftKeyDown() && isSolarHelmet(heldStack)) {
                    event.setCanceled(true);
                    disableSolarHelmet(heldStack);
                    event.getEntity().displayClientMessage(Component.translatable("item.solarhelmet:solar_helmet_module_removing_done.text"), true);
                    if (!event.getEntity().addItem(SOLAR_MODULE_ITEM.get().getDefaultInstance())) {
                        event.getEntity().drop(SOLAR_MODULE_ITEM.get().getDefaultInstance(), false);
                    }
                }
            } else {
                ItemStack helmetStack = getHelmet(event.getEntity());
                // add module to wear helmet
                if (isItemHelmet(helmetStack) && !isSolarHelmet(heldStack)) {
                    if(heldStack.is(SOLAR_MODULE_ITEM.get())){
                        enableSolarHelmet(helmetStack);
                        if (!event.getEntity().isCreative()) {
                            heldStack.shrink(1);
                        }
                        event.setCancellationResult(InteractionResult.SUCCESS);
                        event.getEntity().displayClientMessage(Component.translatable("item.solarhelmet:solar_helmet_module_applying_done.text"), true);
                    }
                }
            }
        }
    }

    public static boolean isItemHelmet(ItemStack stack){
        if(SolarHelmetConfig.GENERAL.HELMET_WHITELIST.get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())){
            return true;
        }
        if(SolarHelmetConfig.GENERAL.HELMET_BLACKLIST.get().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())){
            return false;
        }
        if(stack.getItem() instanceof StandingAndWallBlockItem){ // Heads
            return false;
        }
        if(!stack.has(DataComponents.EQUIPPABLE)){
            return false;
        }
        return stack.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.HEAD;
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
        if(state.getBlock() instanceof SpecialPlantable){
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
    public static void anvilRepair(AnvilCraftEvent.Pre event){
        ItemStack inputStack = event.getLeft();
        ItemStack resultStack = event.getOutput();
        
        if(inputStack.has(DC_SOLAR_HELMET)){
            resultStack.set(DC_SOLAR_HELMET, inputStack.get(DC_SOLAR_HELMET));
            resultStack.set(DC_SOLAR_HELMET_ENERGY, inputStack.getOrDefault(DC_SOLAR_HELMET_ENERGY, 0));
        }
        
    }
    
    public static NonNullList<ItemStack> getInventory(Player player){
        NonNullList<ItemStack> mergedInventory = NonNullList.create();
        for (ItemStack itemStack : player.getInventory()) {
            mergedInventory.add(itemStack);
        }
        return mergedInventory;
    }

    public static boolean isSolarHelmet(ItemStack stack){
        return stack.has(DC_SOLAR_HELMET);
    }

    public static void enableSolarHelmet(ItemStack stack){
        stack.set(DC_SOLAR_HELMET, true);
    }

    public static void disableSolarHelmet(ItemStack stack) {
        stack.remove(DC_SOLAR_HELMET);
    }

    public static ItemStack getHelmet(Player player){
        return player.getInventory().getItem(Inventory.INVENTORY_SIZE + EquipmentSlot.HEAD.getIndex());
    }

}