package de.canitzp.solarhelmet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.block.BlockState;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author canitzp
 */
@Mod(SolarHelmet.MODID)
public class SolarHelmet{
    
    public static final String MODID = "solarhelmet";
    
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<ItemSolarModule> SOLAR_MODULE = ITEMS.register("solar_helmet_module", ItemSolarModule::new);
    
    public static final ItemGroup TAB = new ItemGroup(MODID){
        @Override
        public ItemStack makeIcon(){
            return new ItemStack(SOLAR_MODULE.get());
        }
    
        @Override
        public void fillItemList(NonNullList<ItemStack> stacks){
            stacks.add(new ItemStack(SOLAR_MODULE.get()));
            for(Item item : ForgeRegistries.ITEMS){
                if(SolarHelmet.isItemHelmet(item)){
                    ItemStack stack = new ItemStack(item);
                    CompoundNBT tag = new CompoundNBT();
                    tag.putBoolean("SolarHelmet", true);
                    stack.setTag(tag);
                    stacks.add(stack);
                }
            }
        }
    };
    
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
                CompoundNBT nbt = event.getItemStack().getTag();
                if(nbt != null && nbt.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                    event.getToolTip().add(new TranslationTextComponent("item.solarhelmet:solar_helmet_module_installed.text").withStyle(TextFormatting.AQUA, TextFormatting.ITALIC));
                    if(SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get() > 0){
                        event.getToolTip().add(new TranslationTextComponent("item.solarhelmet:solar_helmet_energy.text", nbt.getInt("solar_helmet_energy_stored"), SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()).withStyle(TextFormatting.RED));
                    }
                }
            }
        }
        
        @SubscribeEvent
        public static void resourceReload(AddReloadListenerEvent event){
            event.addListener(new ReloadListener<RecipeManager>() {
                @Override
                protected RecipeManager prepare(IResourceManager iResourceManager, IProfiler iProfiler){
                    return event.getDataPackRegistries().getRecipeManager();
                }
    
                @Override
                protected void apply(RecipeManager recipeManager, IResourceManager iResourceManager, IProfiler iProfiler){
                    createHelmetRecipes(recipeManager);
                }
            });
        }
    
        private static void createHelmetRecipes(RecipeManager recipeManager){
            LOGGER.info("Solar Helmet recipe injecting...");
    
            // list which the old recipes are replaced with. This should include all existing recipes and the new ones, before recipeManager#replaceRecipes is called!
            List<IRecipe<?>> allNewRecipes = new ArrayList<>();
            NonNullList<Ingredient> ingredients = NonNullList.create();
            ingredients.add(Ingredient.of(SOLAR_MODULE.get()));
            List<String> add_craft_items = SolarHelmetConfig.GENERAL.ADD_CRAFT_ITEMS.get();
            add_craft_items.stream()
                           .limit(7)
                           .map(s -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(s)))
                           .filter(Objects::nonNull)
                           .map(Ingredient::of)
                           .forEach(ingredients::add);
            for(Item item : ForgeRegistries.ITEMS){
                if(!isItemHelmet(item)){
                    continue;
                }
        
                NonNullList<Ingredient> specififcIngredients = NonNullList.create();
                specififcIngredients.addAll(ingredients);
                specififcIngredients.add(Ingredient.of(item));
        
                ItemStack output = item.getDefaultInstance();
                CompoundNBT nbt = new CompoundNBT();
                nbt.putBoolean("SolarHelmet", true);
                output.setTag(nbt);
        
                ResourceLocation craftingId = new ResourceLocation(MODID, "solar_helmet_" + item.getRegistryName().getNamespace() + "_" + item.getRegistryName().getPath());
        
                ShapelessRecipe recipe = new ShapelessRecipe(craftingId, "", output, specififcIngredients) {
                    @Nonnull
                    @Override
                    public ItemStack assemble(CraftingInventory inv){
                        CompoundNBT nbt = new CompoundNBT();
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
            
                    @Override
                    public boolean matches(CraftingInventory inv, World worldIn){
                        if(super.matches(inv, worldIn)){
                            for(int i = 0; i < inv.getContainerSize(); i++){
                                ItemStack stack = inv.getItem(i);
                                if(!stack.isEmpty() && stack.getItem() instanceof ArmorItem){
                                    CompoundNBT nbt = stack.getTag();
                                    if(nbt != null && nbt.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                                        return false;
                                    }
                                }
                            }
                        }
                        return super.matches(inv, worldIn);
                    }
                };
                if(recipeManager.getRecipeIds().noneMatch(resourceLocation -> resourceLocation.equals(craftingId))){
                    allNewRecipes.add(recipe);
                }
            }
            try{
                // add all existing recipes, since we're gonna replace them
                allNewRecipes.addAll(recipeManager.getRecipes());
                replaceRecipes(recipeManager, allNewRecipes);
            } catch(IllegalStateException e){
                LOGGER.error("Solar Helmet: Illegal recipe replacement caught! Report this to author immediately!", e);
            }
        }
    
        @SubscribeEvent
        public static void updatePlayer(TickEvent.PlayerTickEvent event){
            if(event.phase == TickEvent.Phase.END && !event.player.level.isClientSide()){
                PlayerInventory inv = event.player.inventory;
                ItemStack helmet = inv.armor.get(EquipmentSlotType.HEAD.getIndex());
                if(!helmet.isEmpty() && helmet.hasTag() && isItemHelmet(helmet.getItem())){
                    CompoundNBT nbt = helmet.getTag();
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
                                for(ItemStack stack : getInventory(event.player)){ // Check if a item can be recharged
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
        if(item instanceof ArmorItem && ((ArmorItem) item).getSlot() == EquipmentSlotType.HEAD){
            return !SolarHelmetConfig.GENERAL.HELMET_BLACKLIST.get().contains(item.getRegistryName().toString());
        }
        return SolarHelmetConfig.GENERAL.HELMET_WHITELIST.get().contains(item.getRegistryName().toString());
    }
    
    private static boolean isInRightDimension(PlayerEntity player){
        return !SolarHelmetConfig.GENERAL.DIMENSION_BLACKLIST.get().contains(player.level.dimension().location().toString());
    }
    
    private static int getSunlightValue(PlayerEntity player){
        if(player.level.isDay()){
            BlockPos.Mutable pos = new BlockPos.Mutable(player.getX(), player.getY() + 1, player.getZ());
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
    
    private static int getSunlightThrough(World world, BlockPos pos, int currentLightValue){
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
            CompoundNBT inputStackTag = inputStack.getTag();
            CompoundNBT resultStackTag = resultStack.hasTag() ? resultStack.getTag() : new CompoundNBT();
            
            if(inputStackTag.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                resultStackTag.putBoolean("SolarHelmet", inputStackTag.getBoolean("SolarHelmet"));
            }
            
            if(inputStackTag.contains("solar_helmet_energy_stored", Constants.NBT.TAG_INT)){
                resultStackTag.putInt("solar_helmet_energy_stored", inputStackTag.getInt("solar_helmet_energy_stored"));
            }
            
            resultStack.setTag(resultStackTag);
        }
    }
    
    private static Field recipes = ObfuscationReflectionHelper.findField(RecipeManager.class, "field_199522_d"); // RecipeManager.recipes
    public static void replaceRecipes(RecipeManager recipeManager, Collection<IRecipe<?>> allNewRecipes){
        if(recipes != null){
            Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> map = Maps.newHashMap();
            allNewRecipes.forEach((recipe) -> {
                Map<ResourceLocation, IRecipe<?>> map1 = map.computeIfAbsent(recipe.getType(), (p_223390_0_) -> Maps.newHashMap());
                IRecipe<?> overwrittenRecipe = map1.put(recipe.getId(), recipe);
                if (overwrittenRecipe != null) {
                    throw new IllegalStateException("Duplicate recipe ignored with ID " + recipe.getId());
                }
            });
            try{
                recipes.set(recipeManager, ImmutableMap.copyOf(map));
            } catch(IllegalAccessException e){
                e.printStackTrace();
            }
        }
    }
    
    public static NonNullList<ItemStack> getInventory(PlayerEntity player){
        NonNullList<ItemStack> list = NonNullList.create();
        list.addAll(player.inventory.items);
        list.addAll(player.inventory.armor);
        list.addAll(player.inventory.offhand);
        return list;
    }
    
}