package net.minecraftforge.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.fixes.StructuresBecomeConfiguredFix;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.crafting.conditions.ConditionCodec;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.ingredients.IIngredientSerializer;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.BrainBuilder;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.common.util.MavenVersionStringHelper;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.ModMismatchEvent;
import net.minecraftforge.event.RegisterStructureConversionsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.EnderManAngerEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDrownEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingMakeBrainEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.ConnectionType;
import net.minecraftforge.network.ForgePayload;
import net.minecraftforge.network.NetworkContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkInitialization;
import net.minecraftforge.network.NetworkInstance;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.packets.SpawnEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.resource.ResourcePackLoader;
import net.minecraftforge.server.permission.PermissionAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ForgeHooks {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker FORGEHOOKS = MarkerManager.getMarker("FORGEHOOKS");
   private static final Marker WORLDPERSISTENCE = MarkerManager.getMarker("WP");
   static final Pattern URL_PATTERN = Pattern.compile(
      "((?:[a-z0-9]{2,}:\\/\\/)?(?:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}|(?:[-\\w_]{1,}\\.[a-z]{2,}?))(?::[0-9]{1,5})?.*?(?=[!\"§ \n]|$))", 2
   );
   private static final ThreadLocal<Player> CRAFTING_PLAYER = new ThreadLocal<>();
   private static final Map<Holder.Reference<Item>, Integer> VANILLA_BURNS = new HashMap<>();
   private static final Map<EntityType<? extends LivingEntity>, AttributeSupplier> FORGE_ATTRIBUTES = new HashMap<>();
   private static final Lazy<Map<String, StructuresBecomeConfiguredFix.Conversion>> FORGE_CONVERSION_MAP = Lazy.concurrentOf(() -> {
      Map<String, StructuresBecomeConfiguredFix.Conversion> map = new HashMap<>();
      MinecraftForge.EVENT_BUS.post(new RegisterStructureConversionsEvent(map));
      return ImmutableMap.copyOf(map);
   });

   private ForgeHooks() {
   }

   public static boolean canContinueUsing(@NotNull ItemStack from, @NotNull ItemStack to) {
      return !from.isEmpty() && !to.isEmpty() ? from.getItem().canContinueUsing(from, to) : false;
   }

   public static boolean isCorrectToolForDrops(@NotNull BlockState state, @NotNull Player player) {
      return !state.requiresCorrectToolForDrops() ? ForgeEventFactory.doPlayerHarvestCheck(player, state, true) : player.hasCorrectToolForDrops(state);
   }

   public static Brain<?> onLivingMakeBrain(LivingEntity entity, Brain<?> originalBrain, Dynamic<?> dynamic) {
      BrainBuilder<?> brainBuilder = originalBrain.createBuilder();
      LivingMakeBrainEvent event = new LivingMakeBrainEvent(entity, brainBuilder);
      MinecraftForge.EVENT_BUS.post(event);
      return brainBuilder.makeBrain(dynamic);
   }

   public static boolean onLivingAttack(LivingEntity entity, DamageSource src, float amount) {
      return entity instanceof Player || !MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(entity, src, amount));
   }

   public static boolean onPlayerAttack(LivingEntity entity, DamageSource src, float amount) {
      return !MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(entity, src, amount));
   }

   public static boolean onLivingUseTotem(LivingEntity entity, DamageSource damageSource, ItemStack totem, InteractionHand hand) {
      return !MinecraftForge.EVENT_BUS.post(new LivingUseTotemEvent(entity, damageSource, totem, hand));
   }

   public static float onLivingHurt(LivingEntity entity, DamageSource src, float amount) {
      LivingHurtEvent event = new LivingHurtEvent(entity, src, amount);
      return MinecraftForge.EVENT_BUS.post(event) ? 0.0F : event.getAmount();
   }

   public static float onLivingDamage(LivingEntity entity, DamageSource src, float amount) {
      LivingDamageEvent event = new LivingDamageEvent(entity, src, amount);
      return MinecraftForge.EVENT_BUS.post(event) ? 0.0F : event.getAmount();
   }

   public static InteractionResult onInteractEntityAt(Entity entity, Player player, Vec3 vec3d, InteractionHand hand) {
      PlayerInteractEvent.EntityInteractSpecific ret = ForgeEventFactory.onEntityInteractSpecific(player, entity, hand, vec3d);
      return ret.isCanceled() ? ret.getCancellationResult() : entity.interactAt(player, vec3d, hand);
   }

   public static int getLootingLevel(Entity target, @Nullable Entity killer, @Nullable DamageSource cause) {
      int looting = 0;
      if (killer instanceof LivingEntity living) {
         looting = EnchantmentHelper.getEnchantmentLevel(living.level().holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), living);
      }

      if (target instanceof LivingEntity living) {
         looting = ForgeEventFactory.fireLootingLevel(living, cause, looting).getLootingLevel();
      }

      return looting;
   }

   public static double getEntityVisibilityMultiplier(LivingEntity entity, Entity lookingEntity, double originalMultiplier) {
      LivingEvent.LivingVisibilityEvent event = new LivingEvent.LivingVisibilityEvent(entity, lookingEntity, originalMultiplier);
      MinecraftForge.EVENT_BUS.post(event);
      return Math.max(0.0, event.getVisibilityModifier());
   }

   public static Optional<BlockPos> isLivingOnLadder(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull LivingEntity entity) {
      boolean isSpectator = entity instanceof Player && entity.isSpectator();
      if (isSpectator) {
         return Optional.empty();
      } else if (!ForgeConfig.SERVER.fullBoundingBoxLadders.get()) {
         return state.isLadder(level, pos, entity) ? Optional.of(pos) : Optional.empty();
      } else {
         AABB bb = entity.getBoundingBox();
         int mX = Mth.floor(bb.minX);
         int mY = Mth.floor(bb.minY);
         int mZ = Mth.floor(bb.minZ);

         for (int y2 = mY; (double)y2 < bb.maxY; y2++) {
            for (int x2 = mX; (double)x2 < bb.maxX; x2++) {
               for (int z2 = mZ; (double)z2 < bb.maxZ; z2++) {
                  BlockPos tmp = new BlockPos(x2, y2, z2);
                  state = level.getBlockState(tmp);
                  if (state.isLadder(level, tmp, entity)) {
                     return Optional.of(tmp);
                  }
               }
            }
         }

         return Optional.empty();
      }
   }

   public static void onLivingJump(LivingEntity entity) {
      MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(entity));
   }

   @Nullable
   public static ItemEntity onPlayerTossEvent(@NotNull Player player, @NotNull ItemStack item, boolean includeName) {
      player.captureDrops(Lists.newArrayList());
      ItemEntity ret = player.drop(item, false, includeName);
      player.captureDrops(null);
      if (ret == null) {
         return null;
      } else {
         ItemTossEvent event = new ItemTossEvent(ret, player);
         if (MinecraftForge.EVENT_BUS.post(event)) {
            return null;
         } else {
            if (!player.level().isClientSide) {
               player.getCommandSenderWorld().addFreshEntity(event.getEntity());
            }

            return event.getEntity();
         }
      }
   }

   @Nullable
   public static Component onServerChatSubmittedEvent(ServerPlayer player, Component message) {
      String plain = message.getContents() instanceof PlainTextContents.LiteralContents literalContents ? literalContents.text() : "";
      ServerChatEvent event = new ServerChatEvent(player, plain, message);
      return MinecraftForge.EVENT_BUS.post(event) ? null : event.getMessage();
   }

   public static Component newChatWithLinks(String string) {
      return newChatWithLinks(string, true);
   }

   public static Component newChatWithLinks(String string, boolean allowMissingHeader) {
      MutableComponent ichat = null;
      Matcher matcher = URL_PATTERN.matcher(string);
      int lastEnd = 0;

      while (true) {
         String url;
         MutableComponent link;
         while (true) {
            if (!matcher.find()) {
               String end = string.substring(lastEnd);
               if (ichat == null) {
                  ichat = Component.literal(end);
               } else if (!end.isEmpty()) {
                  ichat.append(Component.literal(string.substring(lastEnd)));
               }

               return ichat;
            }

            int start = matcher.start();
            int end = matcher.end();
            String part = string.substring(lastEnd, start);
            if (!part.isEmpty()) {
               if (ichat == null) {
                  ichat = Component.literal(part);
               } else {
                  ichat.append(part);
               }
            }

            lastEnd = end;
            url = string.substring(start, end);
            link = Component.literal(url);

            try {
               if (new URI(url).getScheme() != null) {
                  break;
               }

               if (allowMissingHeader) {
                  url = "http://" + url;
                  break;
               }

               if (ichat == null) {
                  ichat = Component.literal(url);
               } else {
                  ichat.append(url);
               }
            } catch (URISyntaxException var11) {
               if (ichat == null) {
                  ichat = Component.literal(url);
               } else {
                  ichat.append(url);
               }
            }
         }

         ClickEvent click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
         link.setStyle(link.getStyle().withClickEvent(click).withUnderlined(true).withColor(TextColor.fromLegacyFormat(ChatFormatting.BLUE)));
         if (ichat == null) {
            ichat = Component.literal("");
         }

         ichat.append(link);
      }
   }

   public static void dropXpForBlock(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack) {
      HolderLookup<Enchantment> lookup = level.holderLookup(Registries.ENCHANTMENT);
      int fortuneLevel = EnchantmentHelper.getItemEnchantmentLevel(lookup.getOrThrow(Enchantments.FORTUNE), stack);
      int silkTouchLevel = EnchantmentHelper.getItemEnchantmentLevel(lookup.getOrThrow(Enchantments.SILK_TOUCH), stack);
      int exp = state.getExpDrop(level, level.random, pos, fortuneLevel, silkTouchLevel);
      if (exp > 0) {
         state.getBlock().popExperience(level, pos, exp);
      }
   }

   public static int onBlockBreakEvent(Level level, GameType gameType, ServerPlayer entityPlayer, BlockPos pos) {
      boolean preCancelEvent = false;
      ItemStack itemstack = entityPlayer.getMainHandItem();
      if (!itemstack.isEmpty() && !itemstack.getItem().canAttackBlock(level.getBlockState(pos), level, pos, entityPlayer)) {
         preCancelEvent = true;
      }

      if (entityPlayer.blockActionRestricted(level, pos, gameType)) {
         preCancelEvent = true;
      }

      BlockState state = level.getBlockState(pos);
      if (level.getBlockEntity(pos) == null) {
         level.sendBlockUpdated(pos, state, state, 3);
      }

      BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, entityPlayer);
      event.setCanceled(preCancelEvent);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         entityPlayer.connection.send(new ClientboundBlockUpdatePacket(level, pos));
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity != null) {
            Packet<?> pkt = blockEntity.getUpdatePacket();
            if (pkt != null) {
               entityPlayer.connection.send(pkt);
            }
         }
      }

      return event.isCanceled() ? -1 : event.getExpToDrop();
   }

   public static InteractionResult onPlaceItemIntoWorld(@NotNull UseOnContext context) {
      ItemStack itemstack = context.getItemInHand();
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (player != null
         && !player.getAbilities().mayBuild
         && !itemstack.canPlaceOnBlockInAdventureMode(new BlockInWorld(level, context.getClickedPos(), false))) {
         return InteractionResult.PASS;
      } else {
         if (!(itemstack.getItem() instanceof BucketItem)) {
            level.captureBlockSnapshots = true;
         }

         ItemStack preUse = itemstack.copy();
         InteractionResult ret = itemstack.getItem().useOn(context);
         if (itemstack.isEmpty()) {
            ForgeEventFactory.onPlayerDestroyItem(player, preUse, context.getHand());
         }

         level.captureBlockSnapshots = false;
         if (ret.consumesAction()) {
            ItemStack postUse = player.getItemInHand(context.getHand());
            ArrayList<BlockSnapshot> blockSnapshots = new ArrayList<>(level.capturedBlockSnapshots);
            level.capturedBlockSnapshots.clear();
            player.setItemInHand(context.getHand(), preUse);
            Direction side = context.getClickedFace();
            boolean eventResult = false;
            if (blockSnapshots.size() > 1) {
               eventResult = ForgeEventFactory.onMultiBlockPlace(player, blockSnapshots, side);
            } else if (blockSnapshots.size() == 1) {
               eventResult = ForgeEventFactory.onBlockPlace(player, blockSnapshots.get(0), side);
            }

            if (eventResult) {
               ret = InteractionResult.FAIL;

               for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots)) {
                  level.restoringBlockSnapshots = true;
                  blocksnapshot.restore(true, false);
                  level.restoringBlockSnapshots = false;
               }
            } else {
               player.setItemInHand(context.getHand(), postUse);

               for (BlockSnapshot snap : blockSnapshots) {
                  int updateFlag = snap.getFlag();
                  BlockState oldBlock = snap.getReplacedBlock();
                  BlockState newBlock = level.getBlockState(snap.getPos());
                  newBlock.onPlace(level, snap.getPos(), oldBlock, false);
                  level.markAndNotifyBlock(snap.getPos(), level.getChunkAt(snap.getPos()), oldBlock, newBlock, updateFlag, 512);
               }

               if (player != null) {
                  player.awardStat(Stats.ITEM_USED.get(preUse.getItem()));
               }
            }
         }

         level.capturedBlockSnapshots.clear();
         return ret;
      }
   }

   public static boolean onAnvilChange(
      AnvilMenu container, @NotNull ItemStack left, @NotNull ItemStack right, Container outputSlot, String name, long baseCost, Player player
   ) {
      AnvilUpdateEvent e = new AnvilUpdateEvent(left, right, name, baseCost, player);
      if (MinecraftForge.EVENT_BUS.post(e)) {
         return false;
      } else if (e.getOutput().isEmpty()) {
         return true;
      } else {
         outputSlot.setItem(0, e.getOutput());
         container.setMaximumCost((int)e.getCost());
         container.repairItemCountCost = e.getMaterialCost();
         return false;
      }
   }

   public static boolean onGrindstoneTake(Container inputSlots, ContainerLevelAccess access, Function<Level, Integer> xpFunction) {
      access.execute((l, p) -> {
         int xp = xpFunction.apply(l);
         GrindstoneEvent.OnTakeItem e = new GrindstoneEvent.OnTakeItem(inputSlots.getItem(0), inputSlots.getItem(1), xp);
         if (!MinecraftForge.EVENT_BUS.post(e)) {
            if (l instanceof ServerLevel server) {
               ExperienceOrb.award(server, Vec3.atCenterOf(p), e.getXp());
            }

            l.levelEvent(1042, p, 0);
            inputSlots.setItem(0, e.getNewTopItem());
            inputSlots.setItem(1, e.getNewBottomItem());
            inputSlots.setChanged();
         }
      });
      return true;
   }

   public static void setCraftingPlayer(Player player) {
      CRAFTING_PLAYER.set(player);
   }

   public static Player getCraftingPlayer() {
      return CRAFTING_PLAYER.get();
   }

   @NotNull
   public static ItemStack getCraftingRemainingItem(@NotNull ItemStack stack) {
      if (stack.getItem().hasCraftingRemainingItem(stack)) {
         stack = stack.getItem().getCraftingRemainingItem(stack);
         if (!stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > stack.getMaxDamage()) {
            ForgeEventFactory.onPlayerDestroyItem(CRAFTING_PLAYER.get(), stack, (EquipmentSlot)null);
            return ItemStack.EMPTY;
         } else {
            return stack;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public static boolean onPlayerAttackTarget(Player player, Entity target) {
      if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target))) {
         return false;
      } else {
         ItemStack stack = player.getMainHandItem();
         return stack.isEmpty() || !stack.getItem().onLeftClickEntity(stack, player, target);
      }
   }

   public static InteractionResult onItemRightClick(Player player, InteractionHand hand) {
      PlayerInteractEvent.RightClickItem evt = ForgeEventFactory.onRightClickItem(player, hand);
      return evt.isCanceled() ? evt.getCancellationResult() : null;
   }

   public static GameType onChangeGameType(Player player, GameType currentGameType, GameType newGameType) {
      if (currentGameType == newGameType) {
         return currentGameType;
      } else {
         PlayerEvent.PlayerChangeGameModeEvent evt = new PlayerEvent.PlayerChangeGameModeEvent(player, currentGameType, newGameType);
         return MinecraftForge.EVENT_BUS.post(evt) ? currentGameType : evt.getNewGameMode();
      }
   }

   public static <E extends LootPool> Codec<List<E>> createLootTablePoolCodec(Codec<E> vanilla) {
      final Codec<List<E>> list = vanilla.listOf();
      Decoder<List<E>> decoder = new Decoder<List<E>>() {
         public <T> DataResult<Pair<List<E>, T>> decode(DynamicOps<T> ops, T input) {
            return list.decode(ops, input).map(p -> {
               List<E> decoded = (List<E>)p.getFirst();

               for (int x = 0; x < decoded.size(); x++) {
                  decoded.get(x);
               }

               return p;
            });
         }
      };
      return Codec.of(list, decoder);
   }

   public static FluidType getVanillaFluidType(Fluid fluid) {
      if (fluid == Fluids.EMPTY) {
         return ForgeMod.EMPTY_TYPE.get();
      } else if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
         return ForgeMod.WATER_TYPE.get();
      } else if (fluid != Fluids.LAVA && fluid != Fluids.FLOWING_LAVA) {
         if (!ForgeMod.MILK.filter(milk -> milk == fluid).isPresent() && !ForgeMod.FLOWING_MILK.filter(milk -> milk == fluid).isPresent()) {
            throw new RuntimeException("Mod fluids must override getFluidType.");
         } else {
            return ForgeMod.MILK_TYPE.get();
         }
      } else {
         return ForgeMod.LAVA_TYPE.get();
      }
   }

   public static boolean onCropsGrowPre(Level level, BlockPos pos, BlockState state, boolean def) {
      Result result = ((BlockEvent.CropGrowEvent.Pre)MinecraftForge.EVENT_BUS.fire(new BlockEvent.CropGrowEvent.Pre(level, pos, state))).getResult();
      return result.isAllowed() || def && result.isDefault();
   }

   public static void onCropsGrowPost(Level level, BlockPos pos, BlockState state) {
      MinecraftForge.EVENT_BUS.post(new BlockEvent.CropGrowEvent.Post(level, pos, state, level.getBlockState(pos)));
   }

   @Nullable
   public static CriticalHitEvent getCriticalHit(Player player, Entity target, boolean vanillaCritical, float damageModifier) {
      CriticalHitEvent hitResult = new CriticalHitEvent(player, target, damageModifier, vanillaCritical);
      MinecraftForge.EVENT_BUS.post(hitResult);
      return hitResult.getResult() != Result.ALLOW && (!vanillaCritical || hitResult.getResult() != Result.DEFAULT) ? null : hitResult;
   }

   public static ItemStack getProjectile(LivingEntity entity, ItemStack projectileWeaponItem, ItemStack projectile) {
      LivingGetProjectileEvent event = new LivingGetProjectileEvent(entity, projectileWeaponItem, projectile);
      MinecraftForge.EVENT_BUS.post(event);
      return event.getProjectileItemStack();
   }

   @Nullable
   public static String getDefaultCreatorModId(@NotNull ItemStack itemStack) {
      Item item = itemStack.getItem();
      ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
      String modId = registryName == null ? null : registryName.getNamespace();
      if ("minecraft".equals(modId)) {
         if (item instanceof EnchantedBookItem) {
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);
            if (enchants.size() == 1) {
               Holder<Enchantment> enchant = enchants.keySet().iterator().next();
               Optional<ResourceKey<Enchantment>> name = enchant.unwrapKey();
               if (name.isPresent()) {
                  return name.get().location().getNamespace();
               }
            }
         } else if (itemStack.has(DataComponents.POTION_CONTENTS)) {
            Holder<Potion> potion = itemStack.get(DataComponents.POTION_CONTENTS).potion().orElse(null);
            if (potion != null && potion.unwrapKey().isPresent()) {
               return potion.unwrapKey().get().location().getNamespace();
            }
         } else if (item instanceof SpawnEggItem egg) {
            ResourceLocation resourceLocation = ForgeRegistries.ENTITY_TYPES.getKey(egg.getType(itemStack));
            if (resourceLocation != null) {
               return resourceLocation.getNamespace();
            }
         }
      }

      return modId;
   }

   public static boolean onFarmlandTrample(Level level, BlockPos pos, BlockState state, float fallDistance, Entity entity) {
      if (entity.canTrample(state, pos, fallDistance)) {
         BlockEvent.FarmlandTrampleEvent event = new BlockEvent.FarmlandTrampleEvent(level, pos, state, fallDistance, entity);
         MinecraftForge.EVENT_BUS.post(event);
         return !event.isCanceled();
      } else {
         return false;
      }
   }

   public static int onNoteChange(Level level, BlockPos pos, BlockState state, int old, int _new) {
      NoteBlockEvent.Change event = new NoteBlockEvent.Change(level, pos, state, old, _new);
      return MinecraftForge.EVENT_BUS.post(event) ? -1 : event.getVanillaNoteId();
   }

   public static boolean hasNoElements(Ingredient ingredient) {
      ItemStack[] items = ingredient.getItems();
      if (items.length == 0) {
         return true;
      } else if (items.length != 1) {
         return false;
      } else {
         ItemStack item = items[0];
         return item.getItem() == Items.BARRIER && item.getHoverName() instanceof MutableComponent hoverName && hoverName.getString().startsWith("Empty Tag: ");
      }
   }

   @Nullable
   public static EntityDataSerializer<?> getSerializer(int id, CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> vanilla) {
      EntityDataSerializer<?> serializer = vanilla.byId(id);
      if (serializer == null) {
         ForgeRegistry<EntityDataSerializer<?>> registry = (ForgeRegistry<EntityDataSerializer<?>>)ForgeRegistries.ENTITY_DATA_SERIALIZERS.get();
         if (registry != null) {
            serializer = registry.getValue(id);
         }
      }

      return serializer;
   }

   public static int getSerializerId(EntityDataSerializer<?> serializer, CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> vanilla) {
      int id = vanilla.getId(serializer);
      if (id < 0) {
         ForgeRegistry<EntityDataSerializer<?>> registry = (ForgeRegistry<EntityDataSerializer<?>>)ForgeRegistries.ENTITY_DATA_SERIALIZERS.get();
         if (registry != null) {
            id = registry.getID(serializer);
         }
      }

      return id;
   }

   public static boolean canEntityDestroy(Level level, BlockPos pos, LivingEntity entity) {
      if (!level.isLoaded(pos)) {
         return false;
      } else {
         BlockState state = level.getBlockState(pos);
         return ForgeEventFactory.getMobGriefingEvent(level, entity)
            && state.canEntityDestroy(level, pos, entity)
            && ForgeEventFactory.onEntityDestroyBlock(entity, pos, state);
      }
   }

   public static int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
      if (stack.isEmpty()) {
         return 0;
      } else {
         Item item = stack.getItem();
         int ret = stack.getBurnTime(recipeType);
         return ForgeEventFactory.getItemBurnTime(
            stack, ret == -1 ? VANILLA_BURNS.getOrDefault(ForgeRegistries.ITEMS.getDelegateOrThrow(item), 0) : ret, recipeType
         );
      }
   }

   public static synchronized void updateBurns() {
      VANILLA_BURNS.clear();
      FurnaceBlockEntity.getFuel().forEach((k, v) -> VANILLA_BURNS.put(ForgeRegistries.ITEMS.getDelegateOrThrow(k), v));
   }

   public static ObjectArrayList<ItemStack> modifyLoot(ResourceLocation lootTableId, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
      context.setQueriedLootTableId(lootTableId);

      for (IGlobalLootModifier mod : ForgeInternalHandler.getLootModifierManager().getAllLootMods()) {
         generatedLoot = mod.apply(generatedLoot, context);
      }

      return generatedLoot;
   }

   public static List<String> getModPacks() {
      List<String> modpacks = ResourcePackLoader.getPackNames();
      if (modpacks.isEmpty()) {
         throw new IllegalStateException("Attempted to retrieve mod packs before they were loaded in!");
      } else {
         return modpacks;
      }
   }

   public static List<String> getModPacksWithVanilla() {
      List<String> modpacks = getModPacks();
      modpacks.add("vanilla");
      return modpacks;
   }

   @Deprecated
   public static Map<EntityType<? extends LivingEntity>, AttributeSupplier> getAttributesView() {
      return Collections.unmodifiableMap(FORGE_ATTRIBUTES);
   }

   @Deprecated
   public static void modifyAttributes() {
      ModLoader.get().postEvent(new EntityAttributeCreationEvent(FORGE_ATTRIBUTES));
      Map<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> finalMap = new HashMap<>();
      ModLoader.get().postEvent(new EntityAttributeModificationEvent(finalMap));
      finalMap.forEach((k, v) -> {
         AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>)k);
         AttributeSupplier.Builder newBuilder = supplier != null ? new AttributeSupplier.Builder(supplier) : new AttributeSupplier.Builder();
         newBuilder.combine(v);
         FORGE_ATTRIBUTES.put((EntityType<? extends LivingEntity>)k, newBuilder.build());
      });
   }

   public static void writeAdditionalLevelSaveData(WorldData worldData, CompoundTag levelTag) {
      CompoundTag fmlData = new CompoundTag();
      ListTag modList = new ListTag();
      ModList.get().getMods().forEach(mi -> {
         CompoundTag mod = new CompoundTag();
         mod.putString("ModId", mi.getModId());
         mod.putString("ModVersion", MavenVersionStringHelper.artifactVersionToString(mi.getVersion()));
         modList.add(mod);
      });
      fmlData.put("LoadingModList", modList);
      CompoundTag registries = new CompoundTag();
      fmlData.put("Registries", registries);
      LOGGER.debug(WORLDPERSISTENCE, "Gathering id map for writing to world save {}", worldData.getLevelName());

      for (Entry<ResourceLocation, ForgeRegistry.Snapshot> e : RegistryManager.ACTIVE.takeSnapshot(true).entrySet()) {
         registries.put(e.getKey().toString(), e.getValue().write());
      }

      LOGGER.debug(WORLDPERSISTENCE, "ID Map collection complete {}", worldData.getLevelName());
      levelTag.put("fml", fmlData);
   }

   @Internal
   public static void readAdditionalLevelSaveData(LevelStorageSource.LevelStorageAccess access, LevelStorageSource.LevelDirectory levelDirectory) {
      CompoundTag tag = null;

      try {
         CompoundTag rootTag = access.getDataTagRaw(false);
         tag = rootTag.getCompound("fml");
      } catch (IOException var12) {
         try {
            CompoundTag rootTagx = access.getDataTagRaw(true);
            tag = rootTagx.getCompound("fml");
         } catch (IOException var11) {
            LOGGER.error(WORLDPERSISTENCE, "Failed to read level data.. ", var11);
            return;
         }
      }

      if (tag.contains("LoadingModList")) {
         ListTag modList = tag.getList("LoadingModList", 10);
         Map<String, ArtifactVersion> mismatchedVersions = new HashMap<>(modList.size());
         Map<String, ArtifactVersion> missingVersions = new HashMap<>(modList.size());

         for (int i = 0; i < modList.size(); i++) {
            CompoundTag mod = modList.getCompound(i);
            String modId = mod.getString("ModId");
            if (!Objects.equals("minecraft", modId)) {
               String modVersion = mod.getString("ModVersion");
               DefaultArtifactVersion previousVersion = new DefaultArtifactVersion(modVersion);
               ModList.get().getModContainerById(modId).ifPresentOrElse(container -> {
                  ArtifactVersion loadingVersion = container.getModInfo().getVersion();
                  if (!loadingVersion.equals(previousVersion)) {
                     mismatchedVersions.put(modId, previousVersion);
                  }
               }, () -> missingVersions.put(modId, previousVersion));
            }
         }

         ModMismatchEvent mismatchEvent = new ModMismatchEvent(levelDirectory, mismatchedVersions, missingVersions);
         ModLoader.get().postEvent(mismatchEvent);
         StringBuilder resolved = new StringBuilder("The following mods have version differences that were marked resolved:");
         StringBuilder unresolved = new StringBuilder("The following mods have version differences that were not resolved:");
         mismatchEvent.getResolved()
            .forEachOrdered(
               res -> {
                  String modid = res.modid();
                  ModMismatchEvent.MismatchedVersionInfo diff = res.versionDifference();
                  if (res.wasSelfResolved()) {
                     resolved.append(System.lineSeparator())
                        .append(
                           diff.isMissing()
                              ? "%s (version %s -> MISSING, self-resolved)".formatted(modid, diff.oldVersion())
                              : "%s (version %s -> %s, self-resolved)".formatted(modid, diff.oldVersion(), diff.newVersion())
                        );
                  } else {
                     String resolver = res.resolver().getModId();
                     resolved.append(System.lineSeparator())
                        .append(
                           diff.isMissing()
                              ? "%s (version %s -> MISSING, resolved by %s)".formatted(modid, diff.oldVersion(), resolver)
                              : "%s (version %s -> %s, resolved by %s)".formatted(modid, diff.oldVersion(), diff.newVersion(), resolver)
                        );
                  }
               }
            );
         mismatchEvent.getUnresolved()
            .forEachOrdered(
               unres -> {
                  String modid = unres.modid();
                  ModMismatchEvent.MismatchedVersionInfo diff = unres.versionDifference();
                  unresolved.append(System.lineSeparator())
                     .append(
                        diff.isMissing()
                           ? "%s (version %s -> MISSING)".formatted(modid, diff.oldVersion())
                           : "%s (version %s -> %s)".formatted(modid, diff.oldVersion(), diff.newVersion())
                     );
               }
            );
         if (mismatchEvent.anyResolved()) {
            resolved.append(System.lineSeparator()).append("Things may not work well.");
            LOGGER.debug(WORLDPERSISTENCE, resolved.toString());
         }

         if (mismatchEvent.anyUnresolved()) {
            unresolved.append(System.lineSeparator()).append("Things may not work well.");
            LOGGER.warn(WORLDPERSISTENCE, unresolved.toString());
         }
      }

      Multimap<ResourceLocation, ResourceLocation> failedElements = null;
      if (tag.contains("Registries")) {
         Map<ResourceLocation, ForgeRegistry.Snapshot> snapshot = new HashMap<>();
         CompoundTag regs = tag.getCompound("Registries");

         for (String key : regs.getAllKeys()) {
            snapshot.put(ResourceLocation.parse(key), ForgeRegistry.Snapshot.read(regs.getCompound(key)));
         }

         failedElements = GameData.injectSnapshot(snapshot, true, true);
      }

      if (failedElements != null && !failedElements.isEmpty()) {
         StringBuilder buf = new StringBuilder();
         buf.append("Forge Mod Loader could not load this save.\n\n")
            .append("There are ")
            .append(failedElements.size())
            .append(" unassigned registry entries in this save.\n")
            .append("You will not be able to load until they are present again.\n\n");
         failedElements.asMap().forEach((name, entries) -> {
            buf.append("Missing ").append(name).append(":\n");
            entries.forEach(rl -> buf.append("    ").append(rl).append("\n"));
         });
         LOGGER.error(WORLDPERSISTENCE, buf.toString());
      }
   }

   public static String encodeLifecycle(Lifecycle lifecycle) {
      if (lifecycle == Lifecycle.stable()) {
         return "stable";
      } else if (lifecycle == Lifecycle.experimental()) {
         return "experimental";
      } else if (lifecycle instanceof com.mojang.serialization.Lifecycle.Deprecated dep) {
         return "deprecated=" + dep.since();
      } else {
         throw new IllegalArgumentException("Unknown lifecycle.");
      }
   }

   public static Lifecycle parseLifecycle(String lifecycle) {
      if (lifecycle.equals("stable")) {
         return Lifecycle.stable();
      } else if (lifecycle.equals("experimental")) {
         return Lifecycle.experimental();
      } else if (lifecycle.startsWith("deprecated=")) {
         return Lifecycle.deprecated(Integer.parseInt(lifecycle.substring(lifecycle.indexOf(61) + 1)));
      } else {
         throw new IllegalArgumentException("Unknown lifecycle.");
      }
   }

   public static boolean shouldSuppressEnderManAnger(EnderMan enderMan, Player player, ItemStack mask) {
      return mask.isEnderMask(player, enderMan) || MinecraftForge.EVENT_BUS.post(new EnderManAngerEvent(enderMan, player));
   }

   @Nullable
   public static StructuresBecomeConfiguredFix.Conversion getStructureConversion(String originalBiome) {
      return FORGE_CONVERSION_MAP.get().get(originalBiome);
   }

   public static boolean checkStructureNamespace(String biome) {
      ResourceLocation biomeLocation = ResourceLocation.tryParse(biome);
      return biomeLocation != null && !biomeLocation.getNamespace().equals("minecraft");
   }

   public static boolean canUseEntitySelectors(SharedSuggestionProvider provider) {
      if (EntitySelectorParser.allowSelectors(provider)) {
         return true;
      } else {
         if (provider instanceof CommandSourceStack source && source.source instanceof ServerPlayer player) {
            return PermissionAPI.getPermission(player, ForgeMod.USE_SELECTORS_PERMISSION);
         }

         return false;
      }
   }

   @Internal
   public static <T> HolderLookup.RegistryLookup<T> wrapRegistryLookup(final HolderLookup.RegistryLookup<T> lookup) {
      return new HolderLookup.RegistryLookup.Delegate<T>() {
         @Override
         public HolderLookup.RegistryLookup<T> parent() {
            return lookup;
         }

         @Override
         public Stream<HolderSet.Named<T>> listTags() {
            return Stream.empty();
         }

         @Override
         public Optional<HolderSet.Named<T>> get(TagKey<T> key) {
            return Optional.of(HolderSet.emptyNamed(lookup, key));
         }
      };
   }

   public static void onLivingBreathe(LivingEntity entity, int consumeAirAmount, int refillAirAmount) {
      boolean isAir;
      boolean var10000;
      label89: {
         isAir = entity.getEyeInFluidType().isAir()
            || entity.level().getBlockState(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())).is(Blocks.BUBBLE_COLUMN);
         label74:
         if (entity.canDrownInFluidType(entity.getEyeInFluidType()) && !MobEffectUtil.hasWaterBreathing(entity)) {
            if (entity instanceof Player player && player.getAbilities().invulnerable) {
               break label74;
            }

            var10000 = false;
            break label89;
         }

         var10000 = true;
      }

      boolean canBreathe = var10000;
      LivingBreatheEvent breatheEvent = ForgeEventFactory.onLivingBreathe(entity, isAir || canBreathe, consumeAirAmount, refillAirAmount, isAir);
      if (breatheEvent.canBreathe()) {
         if (breatheEvent.canRefillAir()) {
            entity.setAirSupply(Math.min(entity.getAirSupply() + breatheEvent.getRefillAirAmount(), entity.getMaxAirSupply()));
         }
      } else {
         entity.setAirSupply(entity.getAirSupply() - breatheEvent.getConsumeAirAmount());
      }

      if (entity.getAirSupply() <= -20) {
         LivingDrownEvent drownEvent = ForgeEventFactory.onLivingDrown(entity, entity.getAirSupply() <= -20, 2.0F, 8);
         if (!drownEvent.isCanceled() && drownEvent.isDrowning()) {
            entity.setAirSupply(0);
            Vec3 vec3 = entity.getDeltaMovement();

            for (int i = 0; i < drownEvent.getBubbleCount(); i++) {
               double d2 = entity.getRandom().nextDouble() - entity.getRandom().nextDouble();
               double d3 = entity.getRandom().nextDouble() - entity.getRandom().nextDouble();
               double d4 = entity.getRandom().nextDouble() - entity.getRandom().nextDouble();
               entity.level().addParticle(ParticleTypes.BUBBLE, entity.getX() + d2, entity.getY() + d3, entity.getZ() + d4, vec3.x, vec3.y, vec3.z);
            }

            if (drownEvent.getDamageAmount() > 0.0F) {
               entity.hurt(entity.damageSources().drown(), drownEvent.getDamageAmount());
            }
         }
      }

      if (!isAir
         && !entity.level().isClientSide
         && entity.isPassenger()
         && entity.getVehicle() != null
         && !entity.getVehicle().canBeRiddenUnderFluidType(entity.getEyeInFluidType(), entity)) {
         entity.stopRiding();
      }
   }

   public static void onCreativeModeTabBuildContents(
      CreativeModeTab tab,
      ResourceKey<CreativeModeTab> tabKey,
      CreativeModeTab.DisplayItemsGenerator originalGenerator,
      CreativeModeTab.ItemDisplayParameters params,
      CreativeModeTab.Output output
   ) {
      MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility> entries = new MutableHashedLinkedMap<>(
         ItemStackLinkedSet.TYPE_AND_TAG, (key, left, right) -> CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
      );
      originalGenerator.accept(params, (stack, vis) -> {
         if (stack.getCount() != 1) {
            throw new IllegalArgumentException("The stack count must be 1");
         } else {
            entries.put(stack, vis);
         }
      });
      ModLoader.get().postEvent(new BuildCreativeModeTabContentsEvent(tab, tabKey, params, entries));

      for (Entry<ItemStack, CreativeModeTab.TabVisibility> entry : entries) {
         output.accept(entry.getKey(), entry.getValue());
      }
   }

   @Internal
   public static <B extends FriendlyByteBuf> StreamCodec<B, ? extends CustomPacketPayload> getCustomPayloadCodec(ResourceLocation id, int max) {
      NetworkInstance channel = NetworkRegistry.findTarget(id);
      return channel == null ? DiscardedPayload.codec(id, max) : StreamCodec.ofMember((value, buf) -> value.encoder().accept(buf), buf -> {
         int len = buf.readableBytes();
         if (len >= 0 && len <= max) {
            return ForgePayload.create(id, buf.wrap(buf.readBytes(len)));
         } else {
            throw new IllegalArgumentException("Payload may not be larger then " + max + " bytes");
         }
      });
   }

   @Internal
   public static boolean onCustomPayload(CustomPacketPayload payload, Connection connection) {
      CustomPayloadEvent.Context context = new CustomPayloadEvent.Context(connection);
      return onCustomPayload(new CustomPayloadEvent(payload.type().id(), payload, context, 0));
   }

   @Internal
   public static boolean onCustomPayload(ClientboundCustomQueryPacket packet, Connection connection) {
      CustomPayloadEvent.Context context = new CustomPayloadEvent.Context(connection);
      return onCustomPayload(new CustomPayloadEvent(packet.payload().id(), packet.payload(), context, packet.transactionId()));
   }

   @Internal
   public static boolean onCustomPayload(ServerboundCustomQueryAnswerPacket packet, Connection connection) {
      CustomPayloadEvent.Context context = new CustomPayloadEvent.Context(connection);
      return onCustomPayload(new CustomPayloadEvent(NetworkInitialization.LOGIN_NAME, packet.payload(), context, packet.transactionId()));
   }

   @Internal
   public static boolean onCustomPayload(CustomPayloadEvent event) {
      Connection connection = event.getSource().getConnection();
      LogicalSide expectedSide = connection.getReceiving() == PacketFlow.CLIENTBOUND ? LogicalSide.CLIENT : LogicalSide.SERVER;
      if (expectedSide != EffectiveSide.get()) {
         connection.disconnect(Component.literal("Illegal packet received, terminating connection"));
         return false;
      } else {
         NetworkInstance channel = NetworkRegistry.findTarget(event.getChannel());
         if (channel != null && channel.dispatch(event)) {
            return true;
         } else if (!event.getSource().getPacketHandled()) {
            MinecraftForge.EVENT_BUS.post(event);
            return event.getSource().getPacketHandled();
         } else {
            return false;
         }
      }
   }

   @Internal
   public static void handleClientConfigurationComplete(Connection connection) {
      if (NetworkContext.get(connection).getType() == ConnectionType.VANILLA) {
         LOGGER.info("Connected to a vanilla server. Catching up missing behaviour.");
         ConfigTracker.INSTANCE.loadDefaultServerConfigs();
      } else {
         LOGGER.info("Connected to a modded server.");
      }
   }

   @Internal
   public static Packet<ClientGamePacketListener> getEntitySpawnPacket(Entity entity) {
      if (entity instanceof IEntityAdditionalSpawnData add) {
         return NetworkDirection.PLAY_TO_CLIENT.buildPacket(NetworkInitialization.PLAY, new SpawnEntity(entity));
      } else {
         throw new IllegalArgumentException(entity.getClass() + " is not an instance of " + IEntityAdditionalSpawnData.class);
      }
   }

   @Internal
   public static boolean readAndTestCondition(RegistryOps<JsonElement> ops, JsonObject json) {
      if (!json.has("forge:condition")) {
         return true;
      } else {
         ICondition condition = (ICondition)ICondition.SAFE_CODEC.parse(ops, json.getAsJsonObject("forge:condition")).getOrThrow(JsonParseException::new);
         return condition.test(ConditionCodec.getContext(ops), ops);
      }
   }

   @Internal
   public static void writeCondition(ICondition condition, JsonObject out) {
      if (condition != null) {
         JsonElement data = (JsonElement)ICondition.CODEC.encode(condition, JsonOps.INSTANCE, JsonOps.INSTANCE.empty()).getOrThrow(JsonSyntaxException::new);
         out.add("forge:condition", data);
      }
   }

   @Nullable
   @Internal
   public static JsonObject readConditionalAdvancement(RegistryOps<JsonElement> context, JsonObject json) {
      JsonArray entries = GsonHelper.getAsJsonArray(json, "advancements", null);
      if (entries == null) {
         return readAndTestCondition(context, json) ? json : null;
      } else {
         int idx = 0;

         for (JsonElement ele : entries) {
            if (!ele.isJsonObject()) {
               throw new JsonSyntaxException("Invalid advancement entry at index " + idx + " Must be JsonObject");
            }

            if (readAndTestCondition(context, ele.getAsJsonObject())) {
               return ele.getAsJsonObject();
            }

            idx++;
         }

         return null;
      }
   }

   @Internal
   public static Codec<Ingredient> enhanceIngredientCodec(Codec<Ingredient> vanilla) {
      return Codec.lazyInitialized(
         () -> Codec.either(ForgeRegistries.INGREDIENT_SERIALIZERS.get().getCodec().dispatch(Ingredient::serializer, IIngredientSerializer::codec), vanilla)
               .flatComapMap(
                  i -> i.left().isPresent() ? (Ingredient)i.left().get() : (Ingredient)i.right().get(),
                  i -> DataResult.success(i.isVanilla() ? Either.right(i) : Either.left(i))
               )
      );
   }

   public static StreamCodec<RegistryFriendlyByteBuf, Ingredient> ingredientStreamCodec() {
      return StreamCodec.of((buf, value) -> {
         if (value.isVanilla()) {
            ItemStack[] items = value.getItems();
            ByteBufCodecs.writeCount(buf, items.length, Integer.MAX_VALUE);

            for (ItemStack item : items) {
               ItemStack.STREAM_CODEC.encode(buf, item);
            }
         } else {
            IIngredientSerializer<Ingredient> serializer = value.serializer();
            ResourceLocation key = ForgeRegistries.INGREDIENT_SERIALIZERS.get().getKey(serializer);
            if (key == null) {
               throw new IllegalArgumentException("Tried to write unregistered Ingredient to network: " + value);
            }

            buf.writeVarInt(-1);
            buf.writeResourceLocation(key);
            serializer.write(buf, value);
         }
      }, buf -> {
         int size = ByteBufCodecs.readCount(buf, Integer.MAX_VALUE);
         if (size == -1) {
            ResourceLocation key = buf.readResourceLocation();
            IIngredientSerializer<?> serializer = ForgeRegistries.INGREDIENT_SERIALIZERS.get().getValue(key);
            if (serializer == null) {
               throw new DecoderException("Could not read ingredient of type: " + key);
            } else {
               return serializer.read(buf);
            }
         } else {
            NonNullList<ItemStack> ret = NonNullList.createWithCapacity(size);

            for (int x = 0; x < size; x++) {
               ret.add(ItemStack.STREAM_CODEC.decode(buf));
            }

            return Ingredient.fromValues(ret.stream().map(Ingredient.ItemValue::new));
         }
      });
   }

   @Nullable
   public static DyeColor getDyeColorFromItemStack(ItemStack stack) {
      if (stack.getItem() instanceof DyeItem dye) {
         return dye.getDyeColor();
      } else {
         for (int x = 0; x < DyeColor.BLACK.getId(); x++) {
            DyeColor color = DyeColor.byId(x);
            if (stack.is(color.getTag())) {
               return color;
            }
         }

         return null;
      }
   }

   public static DataComponentMap gatherItemComponents(Item item, DataComponentMap dataComponents) {
      return DataComponentMap.composite(dataComponents, ForgeEventFactory.gatherItemComponentsEvent(item, dataComponents).getDataComponentMap());
   }

   @FunctionalInterface
   public interface BiomeCallbackFunction {
      Biome apply(Biome.ClimateSettings var1, BiomeSpecialEffects var2, BiomeGenerationSettings var3, MobSpawnSettings var4);
   }
}
