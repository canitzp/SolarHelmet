package de.canitzp.solarhelmet;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.ForgeEventFactory;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author canitzp
 */
@Mod(SolarHelmet.MODID)
public class SolarHelmet{
    
    public static final String MODID = "solarhelmet";
    
    public static final ItemSolarModule SOLAR_MODULE = new ItemSolarModule();
    
    public static final ItemGroup TAB = new ItemGroup(MODID){
        @Override
        public ItemStack createIcon(){
            return new ItemStack(SOLAR_MODULE);
        }
    
        @Override
        public void fill(NonNullList<ItemStack> stacks){
            stacks.add(new ItemStack(SOLAR_MODULE));
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
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SolarHelmetConfig.spec);
    }
    
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents{
    
        @SubscribeEvent
        public static void registerItems(RegistryEvent.Register<Item> reg){
            reg.getRegistry().register(SOLAR_MODULE);
        }
    
    }
    
    @Mod.EventBusSubscriber
    public static class ForgeEvents{
    
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void renderTooltips(ItemTooltipEvent event){
            if(!event.getItemStack().isEmpty()){
                CompoundNBT nbt = event.getItemStack().getTag();
                if(nbt != null && nbt.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                    event.getToolTip().add(new StringTextComponent(TextFormatting.AQUA.toString() + TextFormatting.ITALIC.toString() + I18n.format("item.solarhelmet:solar_helmet_module_installed.text") + TextFormatting.RESET.toString()));
                    if(SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get() > 0){
                        event.getToolTip().add(new StringTextComponent(TextFormatting.RED.toString() + I18n.format("item.solarhelmet:solar_helmet_energy.text", nbt.getInt("solar_helmet_energy_stored"), SolarHelmetConfig.GENERAL.ENERGY_STORAGE.get()) + TextFormatting.RESET.toString()));
                    }
                }
            }
        }
    
        @SubscribeEvent
        public static void onWorldLoad(WorldEvent.Load event){
            IWorld iWorld = event.getWorld();
            if(iWorld instanceof World){
                World world = (World) iWorld;
                RecipeManager recipeManager = world.getRecipeManager();
                NonNullList<Ingredient> ingredients = NonNullList.create();
                ingredients.add(Ingredient.fromItems(SOLAR_MODULE));
                List<String> add_craft_items = SolarHelmetConfig.GENERAL.ADD_CRAFT_ITEMS.get();
                add_craft_items.stream()
                               .limit(7)
                               .map(s -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(s)))
                               .filter(Objects::nonNull)
                               .map(Ingredient::fromItems)
                               .forEach(ingredients::add);
                Map<ResourceLocation, IRecipe<?>> recipesToInject = new HashMap<>();
                ForgeRegistries.ITEMS.getValues().stream()
                                     .filter(SolarHelmet::isItemHelmet)
                                     .forEach(helmet -> {
                                         NonNullList<Ingredient> ingredientsCopy = NonNullList.create();
                                         ingredientsCopy.addAll(ingredients);
                                         ingredientsCopy.add(Ingredient.fromItems(helmet));
                                         ItemStack helmetStack = new ItemStack(helmet);
                                         CompoundNBT nbt = new CompoundNBT();
                                         nbt.putBoolean("SolarHelmet", true);
                                         helmetStack.setTag(nbt);
                                         ResourceLocation craftingId = new ResourceLocation(MODID, "solar_helmet_" + helmet.getRegistryName().getPath());
                                         ShapelessRecipe recipe = new ShapelessRecipe(craftingId, "", helmetStack, ingredientsCopy){
                                             @Nonnull
                                             @Override
                                             public ItemStack getCraftingResult(CraftingInventory inv){
                                                 CompoundNBT nbt = new CompoundNBT();
                                                 for(int i = 0; i < inv.getSizeInventory(); i++){
                                                     ItemStack stack = inv.getStackInSlot(i);
                                                     if(!stack.isEmpty() && stack.getItem() instanceof ArmorItem){
                                                         if(stack.hasTag()){
                                                             nbt = stack.getTag();
                                                         }
                                                     }
                                                 }
                                                 ItemStack out = super.getCraftingResult(inv);
                                                 nbt.putBoolean("SolarHelmet", true);
                                                 out.setTag(nbt);
                                                 return out;
                                             }
            
                                             @Override
                                             public boolean matches(CraftingInventory inv, World worldIn){
                                                 if(super.matches(inv, worldIn)){
                                                     for(int i = 0; i < inv.getSizeInventory(); i++){
                                                         ItemStack stack = inv.getStackInSlot(i);
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
                                         if(recipeManager.getKeys().noneMatch(resourceLocation -> resourceLocation.equals(craftingId))){
                                             recipesToInject.put(craftingId, recipe);
                                         }
                                     });
                Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> map = new HashMap<>(recipeManager.recipes);
                Map<ResourceLocation, IRecipe<?>> craftingRecipes = new HashMap<>(map.getOrDefault(IRecipeType.CRAFTING, Collections.emptyMap()));
                craftingRecipes.putAll(recipesToInject);
                map.put(IRecipeType.CRAFTING, ImmutableMap.copyOf(craftingRecipes));
                recipeManager.recipes = ImmutableMap.copyOf(map);
            }
        }
    
        @SubscribeEvent
        public static void updatePlayer(TickEvent.PlayerTickEvent event){
            if(event.phase == TickEvent.Phase.END && !event.player.world.isRemote()){
                PlayerInventory inv = event.player.inventory;
                ItemStack helmet = inv.armorInventory.get(EquipmentSlotType.HEAD.getIndex());
                if(!helmet.isEmpty() && helmet.hasTag() && isItemHelmet(helmet.getItem())){
                    CompoundNBT nbt = helmet.getTag();
                    if(nbt.contains("SolarHelmet", Constants.NBT.TAG_BYTE)){
                        if(isInRightDimension(event.player)){ // Produce energy
                            int sunlightValue = getSunlightValue(event.player);
                            float energyBase = sunlightValue / (event.player.world.getMaxLightLevel() * 1.0F);
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
                                for(ItemStack stack : inv.mainInventory){ // Check if a item can be recharged
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
        if(item instanceof ArmorItem && ((ArmorItem) item).getEquipmentSlot() == EquipmentSlotType.HEAD){
            return !SolarHelmetConfig.GENERAL.HELMET_BLACKLIST.get().contains(item.getRegistryName().toString());
        }
        return SolarHelmetConfig.GENERAL.HELMET_WHITELIST.get().contains(item.getRegistryName().toString());
    }
    
    private static boolean isInRightDimension(PlayerEntity player){
        return !SolarHelmetConfig.GENERAL.DIMENSION_BLACKLIST.get().contains(player.world.func_230315_m_().func_242725_p().toString());
    }
    
    private static int getSunlightValue(PlayerEntity player){
        if(player.world.isDaytime()){
            BlockPos.Mutable pos = new BlockPos.Mutable(player.getPosX(), player.getPosY() + 1, player.getPosZ());
            int lightValuePlayer = player.world.getMaxLightLevel();
            for(int y = pos.getY(); y < player.world.getHeight(); y++){
                pos.setY(y);
                lightValuePlayer = getSunlightThrough(player.world, pos, lightValuePlayer);
                if(lightValuePlayer <= 0){
                    break;
                }
            }
            if(player.world.isRaining()){
            
            }
            return lightValuePlayer;
        }
        return 0;
    }
    
    private static int getSunlightThrough(World world, BlockPos pos, int currentLightValue){
        if(world.isAirBlock(pos)){
            return currentLightValue;
        }
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
        
        return Math.min(currentLightValue, 15 - world.getBlockState(pos).getOpacity(world, pos));
    }
    
}