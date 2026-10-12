package net.minecraftforge.event;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.brewing.BrewingRecipeRegisterEvent;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.brewing.PotionBrewEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDrownEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingPackSizeEvent;
import net.minecraftforge.event.entity.living.LivingSwapItemsEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.living.ZombieEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PermissionsChangedEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.level.AlterGroundEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.event.network.ChannelRegistrationChangeEvent;
import net.minecraftforge.event.network.ConnectionStartEvent;
import net.minecraftforge.event.network.GatherLoginConfigurationTasksEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.event.IModBusEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ForgeEventFactory {
   private static final ModLoader ML = ModLoader.get();

   private ForgeEventFactory() {
   }

   private static boolean post(Event e) {
      return MinecraftForge.EVENT_BUS.post(e);
   }

   private static <E extends Event> E fire(E e) {
      post(e);
      return e;
   }

   private static <T extends Event & IModBusEvent> void postModBus(T e) {
      ML.postEvent(e);
   }

   public static boolean onMultiBlockPlace(@Nullable Entity entity, List<BlockSnapshot> blockSnapshots, Direction direction) {
      BlockSnapshot snap = blockSnapshots.get(0);
      BlockState placedAgainst = snap.getLevel().getBlockState(snap.getPos().relative(direction.getOpposite()));
      BlockEvent.EntityMultiPlaceEvent event = new BlockEvent.EntityMultiPlaceEvent(blockSnapshots, placedAgainst, entity);
      return post(event);
   }

   public static boolean onBlockPlace(@Nullable Entity entity, @NotNull BlockSnapshot blockSnapshot, @NotNull Direction direction) {
      BlockState placedAgainst = blockSnapshot.getLevel().getBlockState(blockSnapshot.getPos().relative(direction.getOpposite()));
      BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(blockSnapshot, placedAgainst, entity);
      return post(event);
   }

   public static BlockEvent.NeighborNotifyEvent onNeighborNotify(
      Level level, BlockPos pos, BlockState state, EnumSet<Direction> notifiedSides, boolean forceRedstoneUpdate
   ) {
      return fire(new BlockEvent.NeighborNotifyEvent(level, pos, state, notifiedSides, forceRedstoneUpdate));
   }

   public static boolean doPlayerHarvestCheck(Player player, BlockState state, boolean success) {
      return fire(new PlayerEvent.HarvestCheck(player, state, success)).canHarvest();
   }

   public static float getBreakSpeed(Player player, BlockState state, float original, BlockPos pos) {
      PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(player, state, original, pos);
      return post(event) ? -1.0F : event.getNewSpeed();
   }

   public static void onPlayerDestroyItem(Player player, @NotNull ItemStack stack, @Nullable InteractionHand hand) {
      onPlayerDestroyItem(player, stack, LivingEntity.getSlotForHand(hand));
   }

   public static void onPlayerDestroyItem(Player player, @NotNull ItemStack stack, @Nullable EquipmentSlot slot) {
      post(new PlayerDestroyItemEvent(player, stack, slot));
   }

   public static boolean checkSpawnPlacements(
      EntityType<?> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random, boolean defaultResult
   ) {
      Result result = fire(new MobSpawnEvent.SpawnPlacementCheck(entityType, level, spawnType, pos, random, defaultResult)).getResult();
      return result == Result.DEFAULT ? defaultResult : result == Result.ALLOW;
   }

   public static boolean checkSpawnPosition(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType) {
      Result result = fire(new MobSpawnEvent.PositionCheck(mob, level, spawnType, null)).getResult();
      return result != Result.DEFAULT ? result == Result.ALLOW : mob.checkSpawnRules(level, spawnType) && mob.checkSpawnObstruction(level);
   }

   public static boolean checkSpawnPositionSpawner(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnData spawnData, BaseSpawner spawner) {
      Result result = fire(new MobSpawnEvent.PositionCheck(mob, level, spawnType, null)).getResult();
      return result != Result.DEFAULT
         ? result == Result.ALLOW
         : (spawnData.getCustomSpawnRules().isPresent() || mob.checkSpawnRules(level, spawnType)) && mob.checkSpawnObstruction(level);
   }

   @Nullable
   public static SpawnGroupData onFinalizeSpawn(
      Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnData
   ) {
      MobSpawnEvent.FinalizeSpawn event = new MobSpawnEvent.FinalizeSpawn(
         mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, spawnType, spawnData, null, null
      );
      boolean cancel = post(event);
      return !cancel ? mob.finalizeSpawn(level, event.getDifficulty(), event.getSpawnType(), event.getSpawnData()) : null;
   }

   @Nullable
   public static MobSpawnEvent.FinalizeSpawn onFinalizeSpawnSpawner(
      Mob mob,
      ServerLevelAccessor level,
      DifficultyInstance difficulty,
      @Nullable SpawnGroupData spawnData,
      @Nullable CompoundTag spawnTag,
      BaseSpawner spawner
   ) {
      return fire(
         new MobSpawnEvent.FinalizeSpawn(mob, level, mob.getX(), mob.getY(), mob.getZ(), difficulty, MobSpawnType.SPAWNER, spawnData, spawnTag, spawner)
      );
   }

   public static Result canEntityDespawn(Mob entity, ServerLevelAccessor level) {
      return fire(new MobSpawnEvent.AllowDespawn(entity, level)).getResult();
   }

   public static int getItemBurnTime(@NotNull ItemStack itemStack, int burnTime, @Nullable RecipeType<?> recipeType) {
      return fire(new FurnaceFuelBurnTimeEvent(itemStack, burnTime, recipeType)).getBurnTime();
   }

   public static int getExperienceDrop(LivingEntity entity, Player attackingPlayer, int originalExperience) {
      LivingExperienceDropEvent event = new LivingExperienceDropEvent(entity, attackingPlayer, originalExperience);
      return post(event) ? 0 : event.getDroppedExperience();
   }

   public static int getMaxSpawnPackSize(Mob entity) {
      LivingPackSizeEvent maxCanSpawnEvent = fire(new LivingPackSizeEvent(entity));
      return maxCanSpawnEvent.getResult() == Result.ALLOW ? maxCanSpawnEvent.getMaxPackSize() : entity.getMaxSpawnClusterSize();
   }

   public static Component getPlayerDisplayName(Player player, Component username) {
      return fire(new PlayerEvent.NameFormat(player, username)).getDisplayname();
   }

   public static Component getPlayerTabListDisplayName(Player player) {
      return fire(new PlayerEvent.TabListNameFormat(player)).getDisplayName();
   }

   public static BlockState fireFluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
      return fire(new BlockEvent.FluidPlaceBlockEvent(level, pos, liquidPos, state)).getNewState();
   }

   public static ItemTooltipEvent onItemTooltip(ItemStack itemStack, @Nullable Player entityPlayer, List<Component> list, TooltipFlag flags) {
      return fire(new ItemTooltipEvent(itemStack, entityPlayer, list, flags));
   }

   public static ZombieEvent.SummonAidEvent fireZombieSummonAid(Zombie zombie, Level level, int x, int y, int z, LivingEntity attacker, double summonChance) {
      return fire(new ZombieEvent.SummonAidEvent(zombie, level, x, y, z, attacker, summonChance));
   }

   public static boolean onEntityStruckByLightning(Entity entity, LightningBolt bolt) {
      return post(new EntityStruckByLightningEvent(entity, bolt));
   }

   public static int onItemUseStart(LivingEntity entity, ItemStack item, int duration) {
      LivingEntityUseItemEvent.Start event = new LivingEntityUseItemEvent.Start(entity, item, duration);
      return post(event) ? -1 : event.getDuration();
   }

   public static int onItemUseTick(LivingEntity entity, ItemStack item, int duration) {
      LivingEntityUseItemEvent.Tick event = new LivingEntityUseItemEvent.Tick(entity, item, duration);
      return post(event) ? -1 : event.getDuration();
   }

   public static boolean onUseItemStop(LivingEntity entity, ItemStack item, int duration) {
      return post(new LivingEntityUseItemEvent.Stop(entity, item, duration));
   }

   public static ItemStack onItemUseFinish(LivingEntity entity, ItemStack item, int duration, ItemStack result) {
      return fire(new LivingEntityUseItemEvent.Finish(entity, item, duration, result)).getResultStack();
   }

   public static void onStartEntityTracking(Entity entity, Player player) {
      post(new PlayerEvent.StartTracking(player, entity));
   }

   public static void onStopEntityTracking(Entity entity, Player player) {
      post(new PlayerEvent.StopTracking(player, entity));
   }

   public static void firePlayerLoadingEvent(Player player, File playerDirectory, String uuidString) {
      post(new PlayerEvent.LoadFromFile(player, playerDirectory, uuidString));
   }

   public static void firePlayerSavingEvent(Player player, File playerDirectory, String uuidString) {
      post(new PlayerEvent.SaveToFile(player, playerDirectory, uuidString));
   }

   public static void firePlayerLoadingEvent(Player player, PlayerDataStorage playerFileData, String uuidString) {
      post(new PlayerEvent.LoadFromFile(player, playerFileData.getPlayerDataFolder(), uuidString));
   }

   @Nullable
   public static BlockState onToolUse(BlockState originalState, UseOnContext context, ToolAction toolAction, boolean simulate) {
      BlockEvent.BlockToolModificationEvent event = new BlockEvent.BlockToolModificationEvent(originalState, context, toolAction, simulate);
      return post(event) ? null : event.getFinalState();
   }

   public static int onApplyBonemeal(@Nullable Player player, Level level, BlockPos pos, BlockState state, ItemStack stack) {
      if (player == null) {
         return 0;
      } else {
         BonemealEvent event = new BonemealEvent(player, level, pos, state, stack);
         if (post(event)) {
            return -1;
         } else if (event.getResult() == Result.ALLOW) {
            if (!level.isClientSide) {
               stack.shrink(1);
            }

            return 1;
         } else {
            return 0;
         }
      }
   }

   @Nullable
   public static InteractionResultHolder<ItemStack> onBucketUse(
      @NotNull Player player, @NotNull Level level, @NotNull ItemStack stack, @Nullable HitResult target
   ) {
      FillBucketEvent event = new FillBucketEvent(player, stack, level, target);
      if (post(event)) {
         return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
      } else if (event.getResult() == Result.ALLOW) {
         if (player.getAbilities().instabuild) {
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
         } else {
            stack.shrink(1);
            if (stack.isEmpty()) {
               return new InteractionResultHolder<>(InteractionResult.SUCCESS, event.getFilledBucket());
            } else {
               if (!player.getInventory().add(event.getFilledBucket())) {
                  player.drop(event.getFilledBucket(), false);
               }

               return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
            }
         }
      } else {
         return null;
      }
   }

   public static PlayLevelSoundEvent.AtEntity onPlaySoundAtEntity(Entity entity, Holder<SoundEvent> name, SoundSource category, float volume, float pitch) {
      return fire(new PlayLevelSoundEvent.AtEntity(entity, name, category, volume, pitch));
   }

   public static PlayLevelSoundEvent.AtPosition onPlaySoundAtPosition(
      Level level, double x, double y, double z, Holder<SoundEvent> name, SoundSource category, float volume, float pitch
   ) {
      return fire(new PlayLevelSoundEvent.AtPosition(level, new Vec3(x, y, z), name, category, volume, pitch));
   }

   public static int onItemExpire(ItemEntity entity, @NotNull ItemStack item) {
      if (item.isEmpty()) {
         return -1;
      } else {
         ItemExpireEvent event = new ItemExpireEvent(entity, item.isEmpty() ? 6000 : item.getItem().getEntityLifespan(item, entity.level()));
         return !post(event) ? -1 : event.getExtraLife();
      }
   }

   public static int onItemPickup(ItemEntity entityItem, Player player) {
      EntityItemPickupEvent event = new EntityItemPickupEvent(player, entityItem);
      if (post(event)) {
         return -1;
      } else {
         return event.getResult() == Result.ALLOW ? 1 : 0;
      }
   }

   public static boolean canMountEntity(Entity entityMounting, Entity entityBeingMounted, boolean isMounting) {
      boolean isCanceled = post(new EntityMountEvent(entityMounting, entityBeingMounted, entityMounting.level(), isMounting));
      if (isCanceled) {
         entityMounting.absMoveTo(entityMounting.getX(), entityMounting.getY(), entityMounting.getZ(), entityMounting.yRotO, entityMounting.xRotO);
         return false;
      } else {
         return true;
      }
   }

   public static boolean onAnimalTame(Animal animal, Player tamer) {
      return post(new AnimalTameEvent(animal, tamer));
   }

   public static boolean onPlayerPickupXp(Player player, ExperienceOrb orb) {
      return post(new PlayerXpEvent.PickupXp(player, orb));
   }

   public static Player.BedSleepingProblem onPlayerSleepInBed(Player player, Optional<BlockPos> pos) {
      return fire(new PlayerSleepInBedEvent(player, pos)).getResultStatus();
   }

   public static void onPlayerWakeup(Player player, boolean wakeImmediately, boolean updateLevel) {
      post(new PlayerWakeUpEvent(player, wakeImmediately, updateLevel));
   }

   public static void onPlayerFall(Player player, float distance, float multiplier) {
      post(new PlayerFlyableFallEvent(player, distance, multiplier));
   }

   public static boolean onPlayerSpawnSet(Player player, ResourceKey<Level> levelKey, BlockPos pos, boolean forced) {
      return post(new PlayerSetSpawnEvent(player, levelKey, pos, forced));
   }

   public static PlayerSpawnPhantomsEvent onPlayerSpawnPhantom(Player player, int phantomsToSpawn) {
      return fire(new PlayerSpawnPhantomsEvent(player, phantomsToSpawn));
   }

   public static void onPlayerClone(Player player, Player oldPlayer, boolean wasDeath) {
      post(new PlayerEvent.Clone(player, oldPlayer, wasDeath));
   }

   public static boolean onExplosionStart(Level level, Explosion explosion) {
      return post(new ExplosionEvent.Start(level, explosion));
   }

   public static void onExplosionDetonate(Level level, Explosion explosion, List<Entity> list, double diameter) {
      post(new ExplosionEvent.Detonate(level, explosion, list));
   }

   public static boolean onCreateWorldSpawn(Level level, ServerLevelData settings) {
      return post(new LevelEvent.CreateSpawnPosition(level, settings));
   }

   public static float onLivingHeal(LivingEntity entity, float amount) {
      LivingHealEvent event = new LivingHealEvent(entity, amount);
      return post(event) ? 0.0F : event.getAmount();
   }

   public static boolean onPotionAttemptBrew(NonNullList<ItemStack> stacks) {
      NonNullList<ItemStack> tmp = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);

      for (int x = 0; x < tmp.size(); x++) {
         tmp.set(x, stacks.get(x).copy());
      }

      PotionBrewEvent.Pre event = new PotionBrewEvent.Pre(tmp);
      if (!post(event)) {
         return false;
      } else {
         boolean changed = false;

         for (int x = 0; x < stacks.size(); x++) {
            changed |= ItemStack.matches(tmp.get(x), stacks.get(x));
            stacks.set(x, event.getItem(x));
         }

         if (changed) {
            onPotionBrewed(stacks);
         }

         return true;
      }
   }

   public static void onPotionBrewed(NonNullList<ItemStack> brewingItemStacks) {
      post(new PotionBrewEvent.Post(brewingItemStacks));
   }

   public static void onPlayerBrewedPotion(Player player, ItemStack stack) {
      post(new PlayerBrewedPotionEvent(player, stack));
   }

   @Nullable
   public static <T extends ICapabilityProvider> CapabilityDispatcher gatherCapabilities(Class<? extends T> type, T provider) {
      return gatherCapabilities(type, provider, null);
   }

   @Nullable
   public static <T extends ICapabilityProvider> CapabilityDispatcher gatherCapabilities(
      Class<? extends T> type, T provider, @Nullable ICapabilityProvider parent
   ) {
      return gatherCapabilities(new AttachCapabilitiesEvent<>(type, provider), parent);
   }

   @Nullable
   private static CapabilityDispatcher gatherCapabilities(AttachCapabilitiesEvent<?> event, @Nullable ICapabilityProvider parent) {
      post(event);
      return event.getCapabilities().isEmpty() && parent == null ? null : new CapabilityDispatcher(event.getCapabilities(), event.getListeners(), parent);
   }

   public static boolean fireSleepingLocationCheck(LivingEntity player, BlockPos sleepingLocation) {
      SleepingLocationCheckEvent evt = new SleepingLocationCheckEvent(player, sleepingLocation);
      post(evt);
      Result canContinueSleep = evt.getResult();
      return canContinueSleep == Result.DEFAULT
         ? player.getSleepingPos().map(pos -> player.level().getBlockState(pos).isBed(player.level(), pos, player)).orElse(false)
         : canContinueSleep == Result.ALLOW;
   }

   public static boolean onSleepingTimeCheck(Player player, Optional<BlockPos> sleepingLocation) {
      SleepingTimeCheckEvent evt = new SleepingTimeCheckEvent(player, sleepingLocation);
      post(evt);
      Result canContinueSleep = evt.getResult();
      return canContinueSleep == Result.DEFAULT ? !player.level().isDay() : canContinueSleep == Result.ALLOW;
   }

   public static InteractionResultHolder<ItemStack> onArrowNock(ItemStack item, Level level, Player player, InteractionHand hand, boolean hasAmmo) {
      ArrowNockEvent event = new ArrowNockEvent(player, item, hand, level, hasAmmo);
      return post(event) ? new InteractionResultHolder<>(InteractionResult.FAIL, item) : event.getAction();
   }

   public static int onArrowLoose(ItemStack stack, Level level, Player player, int charge, boolean hasAmmo) {
      ArrowLooseEvent event = new ArrowLooseEvent(player, stack, level, charge, hasAmmo);
      return post(event) ? -1 : event.getCharge();
   }

   public static ProjectileImpactEvent.ImpactResult onProjectileImpactResult(Projectile projectile, HitResult ray) {
      return fire(new ProjectileImpactEvent(projectile, ray)).getImpactResult();
   }

   public static boolean onProjectileImpact(Projectile projectile, HitResult ray) {
      return onProjectileImpactResult(projectile, ray) != ProjectileImpactEvent.ImpactResult.DEFAULT;
   }

   @Nullable
   public static LootTable onLoadLootTable(ResourceLocation name, LootTable table) {
      LootTableLoadEvent event = new LootTableLoadEvent(name, table);
      return post(event) ? null : event.getTable();
   }

   public static boolean canCreateFluidSource(Level level, BlockPos pos, BlockState state, boolean def) {
      Result result = fire(new BlockEvent.CreateFluidSourceEvent(level, pos, state)).getResult();
      return result == Result.DEFAULT ? def : result == Result.ALLOW;
   }

   public static Optional<PortalShape> onTrySpawnPortal(LevelAccessor level, BlockPos pos, Optional<PortalShape> size) {
      if (!size.isPresent()) {
         return size;
      } else {
         return !post(new BlockEvent.PortalSpawnEvent(level, pos, level.getBlockState(pos), size.get())) ? size : Optional.empty();
      }
   }

   public static int onEnchantmentLevelSet(Level level, BlockPos pos, int enchantRow, int power, ItemStack itemStack, int enchantmentLevel) {
      return fire(new EnchantmentLevelSetEvent(level, pos, enchantRow, power, itemStack, enchantmentLevel)).getEnchantLevel();
   }

   public static boolean onEntityDestroyBlock(LivingEntity entity, BlockPos pos, BlockState state) {
      return !post(new LivingDestroyBlockEvent(entity, pos, state));
   }

   public static boolean getMobGriefingEvent(Level level, @Nullable Entity entity) {
      if (entity == null) {
         return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
      } else {
         Result result = fire(new EntityMobGriefingEvent(entity)).getResult();
         return result == Result.DEFAULT ? level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) : result == Result.ALLOW;
      }
   }

   public static SaplingGrowTreeEvent blockGrowFeature(
      LevelAccessor level, RandomSource randomSource, BlockPos pos, @Nullable Holder<ConfiguredFeature<?, ?>> holder
   ) {
      return fire(new SaplingGrowTreeEvent(level, randomSource, pos, holder));
   }

   public static BlockState alterGround(LevelSimulatedReader level, RandomSource random, BlockPos pos, BlockState altered) {
      return fire(new AlterGroundEvent(level, random, pos, altered)).getNewAlteredState();
   }

   public static void fireChunkTicketLevelUpdated(ServerLevel level, long chunkPos, int oldTicketLevel, int newTicketLevel, @Nullable ChunkHolder chunkHolder) {
      if (oldTicketLevel != newTicketLevel) {
         post(new ChunkTicketLevelUpdatedEvent(level, chunkPos, oldTicketLevel, newTicketLevel, chunkHolder));
      }
   }

   public static void fireChunkWatch(ServerPlayer entity, LevelChunk chunk, ServerLevel level) {
      post(new ChunkWatchEvent.Watch(entity, chunk, level));
   }

   public static void fireChunkUnWatch(ServerPlayer entity, ChunkPos chunkpos, ServerLevel level) {
      post(new ChunkWatchEvent.UnWatch(entity, chunkpos, level));
   }

   public static boolean onPistonMovePre(Level level, BlockPos pos, Direction direction, boolean extending) {
      return post(new PistonEvent.Pre(level, pos, direction, extending ? PistonEvent.PistonMoveType.EXTEND : PistonEvent.PistonMoveType.RETRACT));
   }

   public static boolean onPistonMovePost(Level level, BlockPos pos, Direction direction, boolean extending) {
      return post(new PistonEvent.Post(level, pos, direction, extending ? PistonEvent.PistonMoveType.EXTEND : PistonEvent.PistonMoveType.RETRACT));
   }

   public static long onSleepFinished(ServerLevel level, long newTime, long minTime) {
      return fire(new SleepFinishedTimeEvent(level, newTime, minTime)).getNewTime();
   }

   public static List<PreparableReloadListener> onResourceReload(ReloadableServerResources serverResources, RegistryAccess registryAccess) {
      return fire(new AddReloadListenerEvent(serverResources, registryAccess)).getListeners();
   }

   public static void onCommandRegister(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment, CommandBuildContext context) {
      post(new RegisterCommandsEvent(dispatcher, environment, context));
   }

   public static boolean canLivingConvert(LivingEntity entity, EntityType<? extends LivingEntity> outcome, Consumer<Integer> timer) {
      return !post(new LivingConversionEvent.Pre(entity, outcome, timer));
   }

   public static void onLivingConvert(LivingEntity entity, LivingEntity outcome) {
      post(new LivingConversionEvent.Post(entity, outcome));
   }

   public static BabyEntitySpawnEvent onBabySpawn(Mob parentA, Mob parentB, @Nullable AgeableMob proposedChild) {
      return fire(new BabyEntitySpawnEvent(parentA, parentB, proposedChild));
   }

   public static EntityTeleportEvent.TeleportCommand onEntityTeleportCommand(Entity entity, double targetX, double targetY, double targetZ) {
      return fire(new EntityTeleportEvent.TeleportCommand(entity, targetX, targetY, targetZ));
   }

   public static EntityTeleportEvent.SpreadPlayersCommand onEntityTeleportSpreadPlayersCommand(Entity entity, double targetX, double targetY, double targetZ) {
      return fire(new EntityTeleportEvent.SpreadPlayersCommand(entity, targetX, targetY, targetZ));
   }

   public static EntityTeleportEvent.EnderEntity onEnderTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
      return fire(new EntityTeleportEvent.EnderEntity(entity, targetX, targetY, targetZ));
   }

   public static EntityTeleportEvent.EnderPearl onEnderPearlLand(
      ServerPlayer entity, double targetX, double targetY, double targetZ, ThrownEnderpearl pearlEntity, float attackDamage, HitResult hitResult
   ) {
      return fire(new EntityTeleportEvent.EnderPearl(entity, targetX, targetY, targetZ, pearlEntity, attackDamage, hitResult));
   }

   public static EntityTeleportEvent.ChorusFruit onChorusFruitTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
      return fire(new EntityTeleportEvent.ChorusFruit(entity, targetX, targetY, targetZ));
   }

   public static boolean onPermissionChanged(GameProfile gameProfile, int newLevel, PlayerList playerList) {
      int oldLevel = playerList.getServer().getProfilePermissions(gameProfile);
      ServerPlayer player = playerList.getPlayer(gameProfile.getId());
      return newLevel != oldLevel && player != null ? post(new PermissionsChangedEvent(player, newLevel, oldLevel)) : false;
   }

   public static void onPlayerChangedDimension(Player player, ResourceKey<Level> fromDim, ResourceKey<Level> toDim) {
      post(new PlayerEvent.PlayerChangedDimensionEvent(player, fromDim, toDim));
   }

   public static void firePlayerLoggedIn(Player player) {
      post(new PlayerEvent.PlayerLoggedInEvent(player));
   }

   public static void firePlayerLoggedOut(Player player) {
      post(new PlayerEvent.PlayerLoggedOutEvent(player));
   }

   public static void firePlayerRespawnEvent(Player player, boolean endConquered) {
      post(new PlayerEvent.PlayerRespawnEvent(player, endConquered));
   }

   public static void firePlayerItemPickupEvent(Player player, ItemEntity item, ItemStack clone) {
      post(new PlayerEvent.ItemPickupEvent(player, item, clone));
   }

   public static void firePlayerCraftingEvent(Player player, ItemStack crafted, Container craftMatrix) {
      post(new PlayerEvent.ItemCraftedEvent(player, crafted, craftMatrix));
   }

   public static void firePlayerSmeltedEvent(Player player, ItemStack smelted) {
      post(new PlayerEvent.ItemSmeltedEvent(player, smelted));
   }

   public static void onPlayerPreTick(Player player) {
      post(new TickEvent.PlayerTickEvent.Pre(player));
   }

   public static void onPlayerPostTick(Player player) {
      post(new TickEvent.PlayerTickEvent.Post(player));
   }

   public static void onPreLevelTick(Level level, BooleanSupplier haveTime) {
      post(new TickEvent.LevelTickEvent.Pre(level.isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER, level, haveTime));
   }

   public static void onPostLevelTick(Level level, BooleanSupplier haveTime) {
      post(new TickEvent.LevelTickEvent.Post(level.isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER, level, haveTime));
   }

   public static void onPreClientTick() {
      post(new TickEvent.ClientTickEvent.Pre());
   }

   public static void onPostClientTick() {
      post(new TickEvent.ClientTickEvent.Post());
   }

   public static void onPreServerTick(BooleanSupplier haveTime, MinecraftServer server) {
      post(new TickEvent.ServerTickEvent.Pre(haveTime, server));
   }

   public static void onPostServerTick(BooleanSupplier haveTime, MinecraftServer server) {
      post(new TickEvent.ServerTickEvent.Post(haveTime, server));
   }

   public static WeightedRandomList<MobSpawnSettings.SpawnerData> getPotentialSpawns(
      LevelAccessor level, MobCategory category, BlockPos pos, WeightedRandomList<MobSpawnSettings.SpawnerData> oldList
   ) {
      LevelEvent.PotentialSpawns event = new LevelEvent.PotentialSpawns(level, category, pos, oldList);
      return post(event) ? WeightedRandomList.create() : WeightedRandomList.create(event.getSpawnerDataList());
   }

   public static void onAdvancementEarned(Player player, AdvancementHolder holder) {
      post(new AdvancementEvent.AdvancementEarnEvent(player, holder));
   }

   public static void onAdvancementGrant(Player player, AdvancementHolder holder, AdvancementProgress advancementProgress, String criterion) {
      post(
         new AdvancementEvent.AdvancementProgressEvent(
            player, holder, advancementProgress, criterion, AdvancementEvent.AdvancementProgressEvent.ProgressType.GRANT
         )
      );
   }

   public static void onAdvancementRevoke(Player player, AdvancementHolder holder, AdvancementProgress advancementProgress, String criterion) {
      post(
         new AdvancementEvent.AdvancementProgressEvent(
            player, holder, advancementProgress, criterion, AdvancementEvent.AdvancementProgressEvent.ProgressType.REVOKE
         )
      );
   }

   public static void onEntityConstructing(Entity entity) {
      post(new EntityEvent.EntityConstructing(entity));
   }

   public static void onPlayerOpenContainer(ServerPlayer player, AbstractContainerMenu menu) {
      post(new PlayerContainerEvent.Open(player, menu));
   }

   public static void onPlayerCloseContainer(ServerPlayer player, AbstractContainerMenu menu) {
      post(new PlayerContainerEvent.Close(player, menu));
   }

   public static boolean onTravelToDimension(Entity entity, ResourceKey<Level> dimension) {
      return post(new EntityTravelToDimensionEvent(entity, dimension));
   }

   public static void onChunkUnload(ChunkAccess chunk) {
      post(new ChunkEvent.Unload(chunk));
   }

   public static void onChunkLoad(ChunkAccess chunk, boolean newChunk) {
      post(new ChunkEvent.Load(chunk, newChunk));
   }

   public static void onLevelUnload(Level level) {
      post(new LevelEvent.Unload(level));
   }

   public static void onLevelLoad(Level level) {
      post(new LevelEvent.Load(level));
   }

   public static void onLevelSave(Level level) {
      post(new LevelEvent.Save(level));
   }

   public static void onChunkDataSave(ChunkAccess chunk, LevelAccessor world, CompoundTag data) {
      post(new ChunkDataEvent.Save(chunk, world, data));
   }

   public static void onChunkDataLoad(ChunkAccess chunk, CompoundTag data, ChunkType status) {
      post(new ChunkDataEvent.Load(chunk, data, status));
   }

   public static void onGameShuttingDown() {
      post(new GameShuttingDownEvent());
   }

   public static void gatherLoginConfigTasks(Connection connection, Consumer<ConfigurationTask> add) {
      post(new GatherLoginConfigurationTasksEvent(connection, add));
   }

   public static void onConnectionStart(Connection connection) {
      post(new ConnectionStartEvent(connection));
   }

   public static void onChannelRegistrationChange(Connection connection, ChannelRegistrationChangeEvent.Type changeType, HashSet<ResourceLocation> changed) {
      post(new ChannelRegistrationChangeEvent(connection, changeType, changed));
   }

   public static LivingSwapItemsEvent.Hands onLivingSwapHandItems(LivingEntity entity) {
      return fire(new LivingSwapItemsEvent.Hands(entity));
   }

   public static ShieldBlockEvent onShieldBlock(LivingEntity blocker, DamageSource source, float blocked) {
      return fire(new ShieldBlockEvent(blocker, source, blocked));
   }

   public static void onEntityEnterSection(Entity entity, long packedOldPos, long packedNewPos) {
      post(new EntityEvent.EnteringSection(entity, packedOldPos, packedNewPos));
   }

   public static boolean onLivingTick(LivingEntity entity) {
      return post(new LivingEvent.LivingTickEvent(entity));
   }

   public static LivingFallEvent onLivingFall(LivingEntity entity, float distance, float damageMultiplier) {
      return fire(new LivingFallEvent(entity, distance, damageMultiplier));
   }

   public static LivingBreatheEvent onLivingBreathe(LivingEntity entity, boolean canBreathe, int consumeAirAmount, int refillAirAmount, boolean canRefillAir) {
      return fire(new LivingBreatheEvent(entity, canBreathe, consumeAirAmount, refillAirAmount, canRefillAir));
   }

   public static LivingDrownEvent onLivingDrown(LivingEntity entity, boolean isDrowning, float damageAmount, int bubbleCount) {
      return fire(new LivingDrownEvent(entity, isDrowning, damageAmount, bubbleCount));
   }

   public static LivingKnockBackEvent onLivingKnockBack(LivingEntity target, float strength, double ratioX, double ratioZ) {
      return fire(new LivingKnockBackEvent(target, strength, ratioX, ratioZ));
   }

   public static boolean onLivingDeath(LivingEntity entity, DamageSource src) {
      return post(new LivingDeathEvent(entity, src));
   }

   public static boolean onLivingDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, boolean recentlyHit) {
      return post(new LivingDropsEvent(entity, source, drops, recentlyHit));
   }

   public static void onLeftClickEmpty(Player player) {
      post(new PlayerInteractEvent.LeftClickEmpty(player));
   }

   public static PlayerInteractEvent.LeftClickBlock onLeftClickBlock(Player player, BlockPos pos, Direction face, ServerboundPlayerActionPacket.Action action) {
      return fire(new PlayerInteractEvent.LeftClickBlock(player, pos, face, PlayerInteractEvent.LeftClickBlock.Action.convert(action)));
   }

   public static PlayerInteractEvent.LeftClickBlock onLeftClickBlockHold(Player player, BlockPos pos, Direction face) {
      return fire(new PlayerInteractEvent.LeftClickBlock(player, pos, face, PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD));
   }

   public static PlayerInteractEvent.RightClickBlock onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
      return fire(new PlayerInteractEvent.RightClickBlock(player, hand, pos, hitVec));
   }

   public static PlayerInteractEvent.RightClickItem onRightClickItem(Player player, InteractionHand hand) {
      return fire(new PlayerInteractEvent.RightClickItem(player, hand));
   }

   public static PlayerInteractEvent.EntityInteract onEntityInteract(Player player, Entity entity, InteractionHand hand) {
      return fire(new PlayerInteractEvent.EntityInteract(player, hand, entity));
   }

   public static PlayerInteractEvent.EntityInteractSpecific onEntityInteractSpecific(Player player, Entity entity, InteractionHand hand, Vec3 vec3d) {
      return fire(new PlayerInteractEvent.EntityInteractSpecific(player, hand, entity, vec3d));
   }

   public static void onRightClickEmpty(Player player, InteractionHand hand) {
      post(new PlayerInteractEvent.RightClickEmpty(player, hand));
   }

   public static void addPackFindersServer(Consumer<RepositorySource> consumer) {
      postModBus(new AddPackFindersEvent(PackType.SERVER_DATA, consumer));
   }

   public static boolean onEntityJoinLevel(Entity entity, Level level) {
      return post(new EntityJoinLevelEvent(entity, level));
   }

   public static boolean onEntityJoinLevel(Entity entity, Level level, boolean loadedFromDisk) {
      return post(new EntityJoinLevelEvent(entity, level, loadedFromDisk));
   }

   public static boolean onEntityLeaveLevel(Entity entity, Level level) {
      return post(new EntityLeaveLevelEvent(entity, level));
   }

   public static void onDifficultyChange(Difficulty difficulty, Difficulty oldDifficulty) {
      post(new DifficultyChangeEvent(difficulty, oldDifficulty));
   }

   public static void onTagsUpdated(RegistryAccess registryAccess, boolean fromClientPacket, boolean isIntegratedServerConnection) {
      post(new TagsUpdatedEvent(registryAccess, fromClientPacket, isIntegratedServerConnection));
   }

   public static boolean onLivingAttackEntity(LivingEntity entity, DamageSource src, float amount) {
      return post(new LivingAttackEvent(entity, src, amount));
   }

   public static boolean onVanillaGameEvent(Level level, Holder<GameEvent> vanillaEvent, Vec3 pos, GameEvent.Context context) {
      return post(new VanillaGameEvent(level, vanillaEvent.get(), pos, context));
   }

   public static boolean onLivingEffectExpire(LivingEntity entity, MobEffectInstance effect) {
      return post(new MobEffectEvent.Expired(entity, effect));
   }

   public static boolean onLivingEffectAdd(LivingEntity entity, MobEffectInstance oldEffect, MobEffectInstance newEffect, Entity source) {
      return post(new MobEffectEvent.Added(entity, oldEffect, newEffect, source));
   }

   public static boolean onLivingEffectRemove(LivingEntity entity, MobEffect effect) {
      return post(new MobEffectEvent.Remove(entity, effect));
   }

   public static boolean onLivingEffectRemove(LivingEntity entity, MobEffectInstance effect) {
      return post(new MobEffectEvent.Remove(entity, effect));
   }

   public static MobEffectEvent.Applicable onLivingEffectCanApply(LivingEntity entity, MobEffectInstance effect) {
      return fire(new MobEffectEvent.Applicable(entity, effect));
   }

   public static void onLivingEquipmentChange(LivingEntity entity, EquipmentSlot slot, ItemStack from, ItemStack to) {
      fire(new LivingEquipmentChangeEvent(entity, slot, from, to));
   }

   public static LivingChangeTargetEvent onLivingChangeTargetMob(LivingEntity entity, LivingEntity originalTarget) {
      return fire(new LivingChangeTargetEvent(entity, originalTarget, LivingChangeTargetEvent.LivingTargetType.MOB_TARGET));
   }

   public static LivingChangeTargetEvent onLivingChangeTargetBehavior(LivingEntity entity, LivingEntity originalTarget) {
      return fire(new LivingChangeTargetEvent(entity, originalTarget, LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET));
   }

   public static ItemFishedEvent onPlayerFishedItem(List<ItemStack> stacks, int rodDamage, FishingHook hook) {
      return fire(new ItemFishedEvent(stacks, rodDamage, hook));
   }

   public static PlayerXpEvent.XpChange onPlayerXpChange(Player player, int xp) {
      return fire(new PlayerXpEvent.XpChange(player, xp));
   }

   public static PlayerXpEvent.LevelChange onPlayerLevelChange(Player player, int levels) {
      return fire(new PlayerXpEvent.LevelChange(player, levels));
   }

   public static GrindstoneEvent.OnPlaceItem onGrindstoneChange(@NotNull ItemStack top, @NotNull ItemStack bottom, Container outputSlot, int xp) {
      return fire(new GrindstoneEvent.OnPlaceItem(top, bottom, xp));
   }

   public static void onBrewingRecipeRegister(PotionBrewing.Builder builder, FeatureFlagSet features) {
      fire(new BrewingRecipeRegisterEvent(builder, features));
   }

   public static boolean onItemStackedOn(
      ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess
   ) {
      return post(new ItemStackedOnOtherEvent(carriedItem, stackedOnItem, slot, action, player, carriedSlotAccess));
   }

   public static NoteBlockEvent.Play onNotePlay(Level world, BlockPos pos, BlockState state, int note, NoteBlockInstrument instrument) {
      return fire(new NoteBlockEvent.Play(world, pos, state, note, instrument));
   }

   public static AnvilRepairEvent onAnvilRepair(Player player, @NotNull ItemStack output, @NotNull ItemStack left, @NotNull ItemStack right) {
      return fire(new AnvilRepairEvent(player, left, right, output));
   }

   public static void onPlayerTradeWithVillager(Player player, MerchantOffer offer, AbstractVillager villager) {
      post(new TradeWithVillagerEvent(player, offer, villager));
   }

   public static GatherComponentsEvent.Item gatherItemComponentsEvent(Item item, DataComponentMap dataComponents) {
      return fire(new GatherComponentsEvent.Item(item, dataComponents));
   }

   public static LootingLevelEvent fireLootingLevel(LivingEntity target, @Nullable DamageSource cause, int level) {
      return fire(new LootingLevelEvent(target, cause, level));
   }
}
