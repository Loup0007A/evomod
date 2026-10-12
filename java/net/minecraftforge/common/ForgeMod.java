package net.minecraftforge.common;

import com.mojang.serialization.MapCodec;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.DetectedVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.crafting.ConditionalRecipe;
import net.minecraftforge.common.crafting.conditions.AndCondition;
import net.minecraftforge.common.crafting.conditions.FalseCondition;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ItemExistsCondition;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.OrCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;
import net.minecraftforge.common.crafting.conditions.TrueCondition;
import net.minecraftforge.common.crafting.ingredients.CompoundIngredient;
import net.minecraftforge.common.crafting.ingredients.DifferenceIngredient;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.common.crafting.ingredients.IntersectionIngredient;
import net.minecraftforge.common.crafting.ingredients.PartialNBTIngredient;
import net.minecraftforge.common.crafting.ingredients.StrictNBTIngredient;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeBiomeTagsProvider;
import net.minecraftforge.common.data.ForgeBlockTagsProvider;
import net.minecraftforge.common.data.ForgeEntityTypeTagsProvider;
import net.minecraftforge.common.data.ForgeFluidTagsProvider;
import net.minecraftforge.common.data.ForgeItemTagsProvider;
import net.minecraftforge.common.data.ForgeLootTableProvider;
import net.minecraftforge.common.data.ForgeRecipeProvider;
import net.minecraftforge.common.data.ForgeSpriteSourceProvider;
import net.minecraftforge.common.data.VanillaSoundDefinitionsProvider;
import net.minecraftforge.common.loot.CanToolPerformAction;
import net.minecraftforge.common.loot.LootTableIdCondition;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.common.world.NoneBiomeModifier;
import net.minecraftforge.common.world.NoneStructureModifier;
import net.minecraftforge.common.world.StructureModifier;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.CrashReportCallables;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.VersionChecker;
import net.minecraftforge.fml.IExtensionPoint.DisplayTest;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkInitialization;
import net.minecraftforge.network.tasks.ForgeNetworkConfigurationHandler;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeDeferredRegistriesSetup;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.IdMappingEvent;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.holdersets.AndHolderSet;
import net.minecraftforge.registries.holdersets.AnyHolderSet;
import net.minecraftforge.registries.holdersets.HolderSetType;
import net.minecraftforge.registries.holdersets.NotHolderSet;
import net.minecraftforge.registries.holdersets.OrHolderSet;
import net.minecraftforge.server.command.EnumArgument;
import net.minecraftforge.server.command.ModIdArgument;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import net.minecraftforge.unsafe.UnsafeHacks;
import net.minecraftforge.versions.forge.ForgeVersion;
import net.minecraftforge.versions.mcp.MCPVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;

@Mod("forge")
public class ForgeMod {
   public static final String VERSION_CHECK_CAT = "version_checking";
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker FORGEMOD = MarkerManager.getMarker("FORGEMOD");
   private static final List<DeferredRegister<?>> registries = new ArrayList<>();
   private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = deferred(Registries.COMMAND_ARGUMENT_TYPE);
   private static final DeferredRegister<Attribute> ATTRIBUTES = deferred(ForgeRegistries.Keys.ATTRIBUTES);
   public static final RegistryObject<Attribute> SWIM_SPEED = ATTRIBUTES.register(
      "swim_speed", () -> new RangedAttribute("forge.swim_speed", 1.0, 0.0, 1024.0).setSyncable(true)
   );
   public static final RegistryObject<Attribute> NAMETAG_DISTANCE = ATTRIBUTES.register(
      "nametag_distance", () -> new RangedAttribute("forge.name_tag_distance", 64.0, 0.0, 64.0).setSyncable(true)
   );
   private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = deferred(
      ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS
   );
   private static final DeferredRegister<MapCodec<? extends StructureModifier>> STRUCTURE_MODIFIER_SERIALIZERS = deferred(
      ForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS
   );
   private static final DeferredRegister<HolderSetType> HOLDER_SET_TYPES = deferred(ForgeRegistries.Keys.HOLDER_SET_TYPES);
   public static final RegistryObject<HolderSetType> ANY_HOLDER_SET = HOLDER_SET_TYPES.register("any", () -> AnyHolderSet::codec);
   public static final RegistryObject<HolderSetType> AND_HOLDER_SET = HOLDER_SET_TYPES.register("and", () -> AndHolderSet::codec);
   public static final RegistryObject<HolderSetType> OR_HOLDER_SET = HOLDER_SET_TYPES.register("or", () -> OrHolderSet::codec);
   public static final RegistryObject<HolderSetType> NOT_HOLDER_SET = HOLDER_SET_TYPES.register("not", () -> NotHolderSet::codec);
   private static final DeferredRegister<FluidType> VANILLA_FLUID_TYPES = deferred(ForgeRegistries.Keys.FLUID_TYPES, "minecraft");
   public static final RegistryObject<FluidType> EMPTY_TYPE = VANILLA_FLUID_TYPES.register(
      "empty",
      () -> new FluidType(
            FluidType.Properties.create()
               .descriptionId("block.minecraft.air")
               .motionScale(1.0)
               .canPushEntity(false)
               .canSwim(false)
               .canDrown(false)
               .fallDistanceModifier(1.0F)
               .pathType(null)
               .adjacentPathType(null)
               .density(0)
               .temperature(0)
               .viscosity(0)
         ) {
            @Override
            public void setItemMovement(ItemEntity entity) {
               if (!entity.isNoGravity()) {
                  entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, -0.04, 0.0));
               }
            }
         }
   );
   public static final RegistryObject<FluidType> WATER_TYPE = VANILLA_FLUID_TYPES.register(
      "water",
      () -> new FluidType(
            FluidType.Properties.create()
               .descriptionId("block.minecraft.water")
               .fallDistanceModifier(0.0F)
               .canExtinguish(true)
               .canConvertToSource(true)
               .supportsBoating(true)
               .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
               .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
               .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
               .canHydrate(true)
         ) {
            @Nullable
            @Override
            public PathType getBlockPathType(FluidState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, boolean canFluidLog) {
               return canFluidLog ? super.getBlockPathType(state, level, pos, mob, true) : null;
            }

            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
               consumer.accept(new IClientFluidTypeExtensions() {
                  private static final ResourceLocation UNDERWATER_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
                  private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
                  private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
                  private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

                  @Override
                  public ResourceLocation getStillTexture() {
                     return WATER_STILL;
                  }

                  @Override
                  public ResourceLocation getFlowingTexture() {
                     return WATER_FLOW;
                  }

                  @Nullable
                  @Override
                  public ResourceLocation getOverlayTexture() {
                     return WATER_OVERLAY;
                  }

                  @Override
                  public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                     return UNDERWATER_LOCATION;
                  }

                  @Override
                  public int getTintColor() {
                     return -12618012;
                  }

                  @Override
                  public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                     return BiomeColors.getAverageWaterColor(getter, pos) | 0xFF000000;
                  }
               });
            }
         }
   );
   public static final RegistryObject<FluidType> LAVA_TYPE = VANILLA_FLUID_TYPES.register(
      "lava",
      () -> new FluidType(
            FluidType.Properties.create()
               .descriptionId("block.minecraft.lava")
               .canSwim(false)
               .canDrown(false)
               .pathType(PathType.LAVA)
               .adjacentPathType(null)
               .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
               .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
               .lightLevel(15)
               .density(3000)
               .viscosity(6000)
               .temperature(1300)
         ) {
            @Override
            public double motionScale(Entity entity) {
               return entity.level().dimensionType().ultraWarm() ? 0.007 : 0.0023333333333333335;
            }

            @Override
            public void setItemMovement(ItemEntity entity) {
               Vec3 vec3 = entity.getDeltaMovement();
               entity.setDeltaMovement(vec3.x * 0.95F, vec3.y + (double)(vec3.y < 0.06F ? 5.0E-4F : 0.0F), vec3.z * 0.95F);
            }

            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
               consumer.accept(new IClientFluidTypeExtensions() {
                  private static final ResourceLocation LAVA_STILL = ResourceLocation.withDefaultNamespace("block/lava_still");
                  private static final ResourceLocation LAVA_FLOW = ResourceLocation.withDefaultNamespace("block/lava_flow");

                  @Override
                  public ResourceLocation getStillTexture() {
                     return LAVA_STILL;
                  }

                  @Override
                  public ResourceLocation getFlowingTexture() {
                     return LAVA_FLOW;
                  }
               });
            }
         }
   );
   private static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES = deferred(Registries.LOOT_CONDITION_TYPE);
   private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS = deferred(ForgeRegistries.Keys.CONDITION_SERIALIZERS);
   private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = deferred(ForgeRegistries.Keys.RECIPE_SERIALIZERS);
   private static final DeferredRegister<IIngredientSerializer<?>> INGREDIENT_SERIALIZERS = deferred(ForgeRegistries.Keys.INGREDIENT_SERIALIZERS);
   private static boolean enableMilkFluid = false;
   public static final RegistryObject<SoundEvent> BUCKET_EMPTY_MILK = RegistryObject.create(
      ResourceLocation.withDefaultNamespace("item.bucket.empty_milk"), ForgeRegistries.SOUND_EVENTS
   );
   public static final RegistryObject<SoundEvent> BUCKET_FILL_MILK = RegistryObject.create(
      ResourceLocation.withDefaultNamespace("item.bucket.fill_milk"), ForgeRegistries.SOUND_EVENTS
   );
   public static final RegistryObject<FluidType> MILK_TYPE = RegistryObject.createOptional(
      ResourceLocation.withDefaultNamespace("milk"), ForgeRegistries.Keys.FLUID_TYPES.location(), "minecraft"
   );
   public static final RegistryObject<Fluid> MILK = RegistryObject.create(ResourceLocation.withDefaultNamespace("milk"), ForgeRegistries.FLUIDS);
   public static final RegistryObject<Fluid> FLOWING_MILK = RegistryObject.create(ResourceLocation.withDefaultNamespace("flowing_milk"), ForgeRegistries.FLUIDS);
   private static ForgeMod INSTANCE;
   public static final PermissionNode<Boolean> USE_SELECTORS_PERMISSION = new PermissionNode<>(
      "forge", "use_entity_selectors", PermissionTypes.BOOLEAN, (player, uuid, contexts) -> player != null && player.hasPermissions(2)
   );
   private static Method implAddExportsOrOpens;

   private static <T> DeferredRegister<T> deferred(ResourceKey<Registry<T>> key) {
      return deferred(key, "forge");
   }

   private static <T> DeferredRegister<T> deferred(ResourceKey<Registry<T>> key, String modid) {
      DeferredRegister<T> ret = DeferredRegister.create(key, modid);
      registries.add(ret);
      return ret;
   }

   public static ForgeMod getInstance() {
      return INSTANCE;
   }

   public static void enableMilkFluid() {
      enableMilkFluid = true;
   }

   public ForgeMod() {
      LOGGER.info(
         FORGEMOD, "Forge mod loading, version {}, for MC {} with MCP {}", ForgeVersion.getVersion(), MCPVersion.getMCVersion(), MCPVersion.getMCPVersion()
      );
      INSTANCE = this;
      MinecraftForge.initialize();
      CrashReportCallables.registerCrashCallable("Crash Report UUID", () -> {
         UUID uuid = UUID.randomUUID();
         LOGGER.fatal("Preparing crash report with UUID {}", uuid);
         return uuid.toString();
      });
      this.hackDNSResolver();
      NetworkInitialization.init();
      CrashReportCallables.registerCrashCallable("FML", ForgeVersion::getSpec);
      CrashReportCallables.registerCrashCallable("Forge", () -> ForgeVersion.getGroup() + ":" + ForgeVersion.getVersion());
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      modEventBus.addListener(event -> {
         event.dataPackRegistry(ForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifier.DIRECT_CODEC);
         event.dataPackRegistry(ForgeRegistries.Keys.STRUCTURE_MODIFIERS, StructureModifier.DIRECT_CODEC);
      });
      modEventBus.addListener(this::preInit);
      modEventBus.addListener(this::gatherData);
      modEventBus.addListener(this::loadComplete);
      modEventBus.addListener(this::registerFluids);
      modEventBus.addListener(this::registerVanillaDisplayContexts);
      modEventBus.register(this);

      for (DeferredRegister<?> r : registries) {
         r.register(modEventBus);
      }

      MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
      ModLoadingContext.get().registerConfig(Type.CLIENT, ForgeConfig.clientSpec);
      ModLoadingContext.get().registerConfig(Type.SERVER, ForgeConfig.serverSpec);
      ModLoadingContext.get().registerConfig(Type.COMMON, ForgeConfig.commonSpec);
      modEventBus.register(ForgeConfig.class);
      ForgeDeferredRegistriesSetup.setup(modEventBus);
      ModLoadingContext.get().registerExtensionPoint(DisplayTest.class, () -> new DisplayTest(() -> "ANY", (remote, isServer) -> true));
      StartupMessageManager.addModMessage("Forge version " + ForgeVersion.getVersion());
      MinecraftForge.EVENT_BUS.addListener(VillagerTradingManager::loadTrades);
      MinecraftForge.EVENT_BUS.register(MinecraftForge.INTERNAL_HANDLER);
      MinecraftForge.EVENT_BUS.addListener(this::mappingChanged);
      MinecraftForge.EVENT_BUS.addListener(this::tagsUpdated);
      MinecraftForge.EVENT_BUS.addListener(this::registerPermissionNodes);
      MinecraftForge.EVENT_BUS.register(new ForgeNetworkConfigurationHandler());
      ForgeRegistries.ITEMS.tags().addOptionalTagDefaults(Tags.Items.ENCHANTING_FUELS, Set.of(ForgeRegistries.ITEMS.getDelegateOrThrow(Items.LAPIS_LAZULI)));
      addAlias(
         ForgeRegistries.ATTRIBUTES,
         ResourceLocation.fromNamespaceAndPath("forge", "reach_distance"),
         ResourceLocation.fromNamespaceAndPath("forge", "block_reach")
      );
      addAlias(
         ForgeRegistries.ATTRIBUTES,
         ResourceLocation.fromNamespaceAndPath("forge", "attack_range"),
         ResourceLocation.fromNamespaceAndPath("forge", "entity_reach")
      );
   }

   public void preInit(FMLCommonSetupEvent evt) {
      VersionChecker.startVersionCheck();
   }

   public void loadComplete(FMLLoadCompleteEvent event) {
   }

   public void serverStopping(ServerStoppingEvent evt) {
      WorldWorkerManager.clear();
   }

   public void mappingChanged(IdMappingEvent evt) {
      Ingredient.invalidateAll();
   }

   public void tagsUpdated(TagsUpdatedEvent evt) {
      Ingredient.invalidateAll();
   }

   public void gatherData(GatherDataEvent event) {
      DataGenerator gen = event.getGenerator();
      PackOutput packOutput = gen.getPackOutput();
      CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
      ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
      gen.addProvider(
         true,
         new PackMetadataGenerator(packOutput)
            .add(
               PackMetadataSection.TYPE,
               new PackMetadataSection(
                  Component.translatable("pack.forge.description"), DetectedVersion.BUILT_IN.getPackVersion(PackType.CLIENT_RESOURCES), Optional.empty()
               )
            )
      );
      ForgeBlockTagsProvider blockTags = new ForgeBlockTagsProvider(packOutput, lookupProvider, existingFileHelper);
      gen.addProvider(event.includeServer(), blockTags);
      gen.addProvider(event.includeServer(), new ForgeItemTagsProvider(packOutput, lookupProvider, blockTags.contentsGetter(), existingFileHelper));
      gen.addProvider(event.includeServer(), new ForgeEntityTypeTagsProvider(packOutput, lookupProvider, existingFileHelper));
      gen.addProvider(event.includeServer(), new ForgeFluidTagsProvider(packOutput, lookupProvider, existingFileHelper));
      gen.addProvider(event.includeServer(), new ForgeRecipeProvider(packOutput, lookupProvider));
      gen.addProvider(event.includeServer(), new ForgeLootTableProvider(packOutput, lookupProvider));
      gen.addProvider(event.includeServer(), new ForgeBiomeTagsProvider(packOutput, lookupProvider, existingFileHelper));
      gen.addProvider(event.includeClient(), new ForgeSpriteSourceProvider(packOutput, existingFileHelper));
      gen.addProvider(event.includeClient(), new VanillaSoundDefinitionsProvider(packOutput, existingFileHelper));
   }

   public void registerFluids(RegisterEvent event) {
      if (enableMilkFluid) {
         event.register(ForgeRegistries.Keys.SOUND_EVENTS, helper -> {
            helper.register(BUCKET_EMPTY_MILK.getId(), SoundEvent.createVariableRangeEvent(BUCKET_EMPTY_MILK.getId()));
            helper.register(BUCKET_FILL_MILK.getId(), SoundEvent.createVariableRangeEvent(BUCKET_FILL_MILK.getId()));
         });
         event.register(
            ForgeRegistries.Keys.FLUID_TYPES,
            helper -> helper.register(
                  MILK_TYPE.getId(),
                  new FluidType(
                     FluidType.Properties.create()
                        .density(1024)
                        .viscosity(1024)
                        .sound(SoundActions.BUCKET_FILL, BUCKET_FILL_MILK.get())
                        .sound(SoundActions.BUCKET_EMPTY, BUCKET_EMPTY_MILK.get())
                  ) {
                     @Override
                     public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                        consumer.accept(new IClientFluidTypeExtensions() {
                           private static final ResourceLocation MILK_STILL = ResourceLocation.fromNamespaceAndPath("forge", "block/milk_still");
                           private static final ResourceLocation MILK_FLOW = ResourceLocation.fromNamespaceAndPath("forge", "block/milk_flowing");

                           @Override
                           public ResourceLocation getStillTexture() {
                              return MILK_STILL;
                           }

                           @Override
                           public ResourceLocation getFlowingTexture() {
                              return MILK_FLOW;
                           }
                        });
                     }
                  }
               )
         );
         event.register(ForgeRegistries.Keys.FLUIDS, helper -> {
            ForgeFlowingFluid.Properties properties = new ForgeFlowingFluid.Properties(MILK_TYPE, MILK, FLOWING_MILK).bucket(() -> Items.MILK_BUCKET);
            helper.register(MILK.getId(), new ForgeFlowingFluid.Source(properties));
            helper.register(FLOWING_MILK.getId(), new ForgeFlowingFluid.Flowing(properties));
         });
      }
   }

   public void registerVanillaDisplayContexts(RegisterEvent event) {
      if (event.getRegistryKey().equals(ForgeRegistries.Keys.DISPLAY_CONTEXTS)) {
         IForgeRegistryInternal<ItemDisplayContext> forgeRegistry = (IForgeRegistryInternal<ItemDisplayContext>)event.<ItemDisplayContext>getForgeRegistry();
         if (forgeRegistry == null) {
            throw new IllegalStateException("Item display context was not a forge registry, wtf???");
         }

         Arrays.stream(ItemDisplayContext.values())
            .filter(Predicate.not(ItemDisplayContext::isModded))
            .forEach(ctx -> forgeRegistry.register(ctx.getId(), ResourceLocation.fromNamespaceAndPath("minecraft", ctx.getSerializedName()), ctx));
      }
   }

   public void registerPermissionNodes(PermissionGatherEvent.Nodes event) {
      event.addNodes(USE_SELECTORS_PERMISSION);
   }

   private static <T> void addAlias(IForgeRegistry<T> registry, ResourceLocation from, ResourceLocation to) {
      ForgeRegistry<T> fReg = (ForgeRegistry<T>)registry;
      fReg.addAlias(from, to);
   }

   private void hackDNSResolver() {
      try {
         Class<?> target = Class.forName("com.sun.jndi.dns.DnsContextFactory");
         Class<?> reader = Class.forName("javax.naming.spi.NamingManager");
         addOpen(target, reader);
      } catch (Exception var3) {
         LOGGER.error(FORGEMOD, "Failed to hack DnsContextFactory, some servers might not work", var3);
      }
   }

   private static void addOpen(Class<?> target, Class<?> reader) throws Exception {
      if (implAddExportsOrOpens == null) {
         implAddExportsOrOpens = Module.class.getDeclaredMethod("implAddExportsOrOpens", String.class, Module.class, boolean.class, boolean.class);
         UnsafeHacks.setAccessible(implAddExportsOrOpens);
      }

      LOGGER.info(FORGEMOD, "Opening {}/{} to {}", target.getModule().getName(), target.getPackageName(), reader.getModule().getName());
      implAddExportsOrOpens.invoke(target.getModule(), target.getPackageName(), reader.getModule(), true, true);
   }

   static {
      RegistryObject<EnumArgument.Info> v = COMMAND_ARGUMENT_TYPES.register(
         "enum", () -> ArgumentTypeInfos.registerByClass(EnumArgument.class, new EnumArgument.Info())
      );
      COMMAND_ARGUMENT_TYPES.register(
         "modid", () -> ArgumentTypeInfos.registerByClass(ModIdArgument.class, SingletonArgumentInfo.contextFree(ModIdArgument::modIdArgument))
      );
      BIOME_MODIFIER_SERIALIZERS.register("none", () -> NoneBiomeModifier.CODEC);
      BIOME_MODIFIER_SERIALIZERS.register("add_features", () -> ForgeBiomeModifiers.AddFeaturesBiomeModifier.CODEC);
      BIOME_MODIFIER_SERIALIZERS.register("remove_features", () -> ForgeBiomeModifiers.RemoveFeaturesBiomeModifier.CODEC);
      BIOME_MODIFIER_SERIALIZERS.register("add_spawns", () -> ForgeBiomeModifiers.AddSpawnsBiomeModifier.CODEC);
      BIOME_MODIFIER_SERIALIZERS.register("remove_spawns", () -> ForgeBiomeModifiers.RemoveSpawnsBiomeModifier.CODEC);
      STRUCTURE_MODIFIER_SERIALIZERS.register("none", () -> NoneStructureModifier.CODEC);
      LOOT_CONDITION_TYPES.register("loot_table_id", () -> LootTableIdCondition.TYPE);
      LOOT_CONDITION_TYPES.register("can_tool_perform_action", () -> CanToolPerformAction.TYPE);
      CONDITION_SERIALIZERS.register("and", () -> AndCondition.CODEC);
      CONDITION_SERIALIZERS.register("false", () -> FalseCondition.CODEC);
      CONDITION_SERIALIZERS.register("item_exists", () -> ItemExistsCondition.CODEC);
      CONDITION_SERIALIZERS.register("mod_loaded", () -> ModLoadedCondition.CODEC);
      CONDITION_SERIALIZERS.register("not", () -> NotCondition.CODEC);
      CONDITION_SERIALIZERS.register("or", () -> OrCondition.CODEC);
      CONDITION_SERIALIZERS.register("true", () -> TrueCondition.CODEC);
      CONDITION_SERIALIZERS.register("tag_empty", () -> TagEmptyCondition.CODEC);
      RECIPE_SERIALIZERS.register("conditional", () -> ConditionalRecipe.SERIALZIER);
      INGREDIENT_SERIALIZERS.register("compound", () -> CompoundIngredient.SERIALIZER);
      INGREDIENT_SERIALIZERS.register("nbt", () -> StrictNBTIngredient.SERIALIZER);
      INGREDIENT_SERIALIZERS.register("partial_nbt", () -> PartialNBTIngredient.SERIALIZER);
      INGREDIENT_SERIALIZERS.register("difference", () -> DifferenceIngredient.SERIALIZER);
      INGREDIENT_SERIALIZERS.register("intersection", () -> IntersectionIngredient.SERIALIZER);
   }
}
