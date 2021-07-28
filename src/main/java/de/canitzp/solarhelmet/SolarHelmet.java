package de.canitzp.solarhelmet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author canitzp
 */
@Mod(SolarHelmet.MODID)
public class SolarHelmet{
    
    public static final String MODID = "solarhelmet";
    
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<ItemSolarModule> SOLAR_MODULE_ITEM = ITEMS.register("solar_helmet_module", ItemSolarModule::new);
    
    public static final CreativeModeTab TAB = new CreativeModeTab(MODID){
        @Override
        public ItemStack makeIcon(){
            return new ItemStack(SOLAR_MODULE_ITEM.get());
        }
    
        @Override
        public void fillItemList(NonNullList<ItemStack> stacks){
            stacks.add(SOLAR_MODULE_ITEM.get().getDefaultInstance());
            for(Item item : ForgeRegistries.ITEMS){
                if(SolarHelmet.isItemHelmet(item)){
                    ItemStack stack = new ItemStack(item);
                    CompoundTag tag = new CompoundTag();
                    tag.putBoolean("SolarHelmet", true);
                    stack.setTag(tag);
                    stacks.add(stack);
                }
            }
        }
    };
    
    public SolarHelmet(){
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SolarHelmetConfig.spec);
        
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
    
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents{
    
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> reg){
            reg.getRegistry().register(SOLAR_MODULE_ITEM.get());
        }
    
    }
    
    @Mod.EventBusSubscriber
    public static class ForgeEvents{
    
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void renderTooltips(ItemTooltipEvent event){
            if(!event.getItemStack().isEmpty()){
                CompoundTag nbt = event.getItemStack().getTag();
                if(nbt != null && nbt.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                    event.getToolTip().add(new TranslatableComponent("item.solarhelmet:solar_helmet_module_installed.text").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                    if(SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get() > 0){
                        event.getToolTip().add(new TranslatableComponent("item.solarhelmet:solar_helmet_energy.text", nbt.getInt("solar_helmet_energy_stored"), SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()).withStyle(ChatFormatting.RED));
                    }
                }
            }
        }
    
        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Load event){
            LevelAccessor iWorld = event.getWorld();
            if(iWorld instanceof Level level){
                if(level.dimension() != Level.OVERWORLD){
                    return;
                }
                RecipeManager recipeManager = level.getRecipeManager();
    
                List<Recipe<?>> newRecipes = new ArrayList<>();
    
                for(Item helmet : ForgeRegistries.ITEMS.getValues()){
                    if(SolarHelmet.isItemHelmet(helmet)){
                        // create recipe input
                        NonNullList<Ingredient> recipeInput = NonNullList.create();
                        recipeInput.add(Ingredient.of(SOLAR_MODULE_ITEM.get()));
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
                        // create recipe id
                        ResourceLocation craftingId = new ResourceLocation(MODID, "solar_helmet_" + helmet.getRegistryName().getNamespace() + "_" + helmet.getRegistryName().getPath());
                        // create recipe
                        ShapelessRecipe recipe = new ShapelessRecipe(craftingId, "", helmetStack, recipeInput) {
                            @Nonnull
                            @Override
                            public ItemStack assemble(CraftingContainer inv){
                                CompoundTag nbt = new CompoundTag();
                                for(int i = 0; i < inv.getContainerSize(); i++){
                                    ItemStack stack = inv.getItem(i);
                                    if(!stack.isEmpty() && stack.getItem() instanceof ArmorItem){
                                        if(stack.hasTag()){
                                            nbt = stack.getTag().copy();
                                        }
                                    }
                                }
                                ItemStack out = super.assemble(inv);
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
                                            if(nbt != null && nbt.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
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
                            newRecipes.add(recipe);
                            System.out.printf("Registering recipes for solar helmet; recipe id: '%s'%n", craftingId);
                        }
                    }
                }
                try{
                    recipeManager.replaceRecipes(newRecipes);
                } catch(IllegalStateException e){
                    System.err.println("Illegal recipe replacement catched!");
                    e.printStackTrace();
                }
            }
        }
    
        @SubscribeEvent
        public static void updatePlayer(TickEvent.PlayerTickEvent event){
            if(event.phase == TickEvent.Phase.END && !event.player.level.isClientSide()){
                Inventory inv = event.player.getInventory();
                ItemStack helmet = inv.armor.get(EquipmentSlot.HEAD.getIndex());
                if(!helmet.isEmpty() && helmet.hasTag() && isItemHelmet(helmet.getItem())){
                    CompoundTag nbt = helmet.getTag();
                    if(nbt.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                        if(isInRightDimension(event.player)){ // Produce energy
                            int sunlightValue = getSunlightValue(event.player);
                            float energyBase = sunlightValue / (event.player.level.getMaxLightLevel() * 1.0F);
                            int producedEnergy = Math.toIntExact(Math.round(energyBase * 100 * SolarHelmetConfig.GENERAL.ENERGY_PRODUCTION_MULTIPLIER.get()));
    
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
    
                        if(nbt.contains("solar_helmet_energy_stored", Constants.NBT.TAG_INT)){ // Consume energy
                            int storedEnergy = nbt.getInt("solar_helmet_energy_stored");
                            if(storedEnergy > 0){
                                AtomicInteger energyLeft = new AtomicInteger(storedEnergy);
                                for(ItemStack stack : inv.items){ // Check if a item can be recharged
                                    stack.getCapability(CapabilityEnergy.ENERGY).ifPresent(energyStorage -> {
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
    
    private static boolean isItemHelmet(Item item){
        if(item instanceof ArmorItem && ((ArmorItem) item).getSlot() == EquipmentSlot.HEAD){
            return !SolarHelmetConfig.GENERAL.HELMET_BLACKLIST.get().contains(item.getRegistryName().toString());
        }
        return SolarHelmetConfig.GENERAL.HELMET_WHITELIST.get().contains(item.getRegistryName().toString());
    }
    
    private static boolean isInRightDimension(Player player){
        return !SolarHelmetConfig.GENERAL.DIMENSION_BLACKLIST.get().contains(player.level.dimension().location().toString());
    }
    
    private static int getSunlightValue(Player player){
        if(player.level.isDay()){
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(player.getBlockX(), player.getBlockY() + 1, player.getBlockZ());
            int lightValuePlayer = player.level.getMaxLightLevel();
            for(int y = pos.getY(); y < player.level.getHeight(); y++){
                pos.setY(y);
                lightValuePlayer = getSunlightThrough(player.level, pos, lightValuePlayer);
                if(lightValuePlayer <= 0){
                    break;
                }
            }
            if(player.level.isRaining()){
            
            }
            return lightValuePlayer;
        }
        return 0;
    }
    
    private static int getSunlightThrough(Level world, BlockPos pos, int currentLightValue){
        BlockState state = world.getBlockState(pos);
        if(state.getBlock() instanceof IPlantable){
            return currentLightValue;
        }
        
        if(SolarHelmetConfig.GENERAL.ADDITIONAL_OPAQUE_BLOCKS.get().contains(state.getBlock().getRegistryName().toString())){
            return currentLightValue;
        }
        if(SolarHelmetConfig.GENERAL.ADDITIONAL_PARTLY_OPAQUE_BLOCKS.get().contains(state.getBlock().getRegistryName().toString())){
            return 1;
        }
        if(SolarHelmetConfig.GENERAL.ADDITIONAL_NON_OPAQUE_BLOCKS.get().contains(state.getBlock().getRegistryName().toString())){
            return 0;
        }
        
        return Math.min(currentLightValue, 15 - world.getBlockState(pos).getLightBlock(world, pos));
    }
    
    // copy solar helmet tag and energy stored to new stack
    @SubscribeEvent
    public static void anvilRepair(AnvilRepairEvent event){
        ItemStack inputStack = event.getItemInput();
        ItemStack resultStack = event.getItemResult();
        
        if(inputStack.hasTag()){
            CompoundTag inputStackTag = inputStack.getTag();
            CompoundTag resultStackTag = resultStack.hasTag() ? resultStack.getTag() : new CompoundTag();
    
            if(inputStackTag.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                resultStackTag.putBoolean("SolarHelmet", inputStackTag.getBoolean("SolarHelmet"));
            }
    
            if(inputStackTag.contains("solar_helmet_energy_stored", Constants.NBT.TAG_INT)){
                resultStackTag.putInt("solar_helmet_energy_stored", inputStackTag.getInt("solar_helmet_energy_stored"));
            }
            
            resultStack.setTag(resultStackTag);
        }
        
    }
    
}