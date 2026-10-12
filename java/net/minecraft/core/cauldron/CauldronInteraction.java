package net.minecraft.core.cauldron;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public interface CauldronInteraction {
   Map<String, CauldronInteraction.InteractionMap> INTERACTIONS = new Object2ObjectArrayMap();
   Codec<CauldronInteraction.InteractionMap> CODEC = Codec.stringResolver(CauldronInteraction.InteractionMap::name, INTERACTIONS::get);
   CauldronInteraction.InteractionMap EMPTY = newInteractionMap("empty");
   CauldronInteraction.InteractionMap WATER = newInteractionMap("water");
   CauldronInteraction.InteractionMap LAVA = newInteractionMap("lava");
   CauldronInteraction.InteractionMap POWDER_SNOW = newInteractionMap("powder_snow");
   CauldronInteraction FILL_WATER = (p_325752_, p_325753_, p_325754_, p_325755_, p_325756_, p_325757_) -> emptyBucket(
         p_325753_,
         p_325754_,
         p_325755_,
         p_325756_,
         p_325757_,
         Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Integer.valueOf(3)),
         SoundEvents.BUCKET_EMPTY
      );
   CauldronInteraction FILL_LAVA = (p_325776_, p_325777_, p_325778_, p_325779_, p_325780_, p_325781_) -> emptyBucket(
         p_325777_, p_325778_, p_325779_, p_325780_, p_325781_, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA
      );
   CauldronInteraction FILL_POWDER_SNOW = (p_325782_, p_325783_, p_325784_, p_325785_, p_325786_, p_325787_) -> emptyBucket(
         p_325783_,
         p_325784_,
         p_325785_,
         p_325786_,
         p_325787_,
         Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Integer.valueOf(3)),
         SoundEvents.BUCKET_EMPTY_POWDER_SNOW
      );
   CauldronInteraction SHULKER_BOX = (p_340986_, p_340987_, p_340988_, p_340989_, p_340990_, p_340991_) -> {
      Block $$6 = Block.byItem(p_340991_.getItem());
      if (!($$6 instanceof ShulkerBoxBlock)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         if (!p_340987_.isClientSide) {
            ItemStack $$7 = p_340991_.transmuteCopy(Blocks.SHULKER_BOX, 1);
            p_340989_.setItemInHand(p_340990_, ItemUtils.createFilledResult(p_340991_, p_340989_, $$7, false));
            p_340989_.awardStat(Stats.CLEAN_SHULKER_BOX);
            LayeredCauldronBlock.lowerFillLevel(p_340986_, p_340987_, p_340988_);
         }

         return ItemInteractionResult.sidedSuccess(p_340987_.isClientSide);
      }
   };
   CauldronInteraction BANNER = (p_340992_, p_340993_, p_340994_, p_340995_, p_340996_, p_340997_) -> {
      BannerPatternLayers $$6 = p_340997_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      if ($$6.layers().isEmpty()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         if (!p_340993_.isClientSide) {
            ItemStack $$7 = p_340997_.copyWithCount(1);
            $$7.set(DataComponents.BANNER_PATTERNS, $$6.removeLast());
            p_340995_.setItemInHand(p_340996_, ItemUtils.createFilledResult(p_340997_, p_340995_, $$7, false));
            p_340995_.awardStat(Stats.CLEAN_BANNER);
            LayeredCauldronBlock.lowerFillLevel(p_340992_, p_340993_, p_340994_);
         }

         return ItemInteractionResult.sidedSuccess(p_340993_.isClientSide);
      }
   };
   CauldronInteraction DYED_ITEM = (p_325770_, p_325771_, p_325772_, p_325773_, p_325774_, p_325775_) -> {
      if (!p_325775_.is(ItemTags.DYEABLE)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (!p_325775_.has(DataComponents.DYED_COLOR)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         if (!p_325771_.isClientSide) {
            p_325775_.remove(DataComponents.DYED_COLOR);
            p_325773_.awardStat(Stats.CLEAN_ARMOR);
            LayeredCauldronBlock.lowerFillLevel(p_325770_, p_325771_, p_325772_);
         }

         return ItemInteractionResult.sidedSuccess(p_325771_.isClientSide);
      }
   };

   static CauldronInteraction.InteractionMap newInteractionMap(String pName) {
      Object2ObjectOpenHashMap<Item, CauldronInteraction> $$1 = new Object2ObjectOpenHashMap();
      $$1.defaultReturnValue(
         (CauldronInteraction)(p_325722_, p_325723_, p_325724_, p_325725_, p_325726_, p_325727_) -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
      );
      CauldronInteraction.InteractionMap $$2 = new CauldronInteraction.InteractionMap(pName, $$1);
      INTERACTIONS.put(pName, $$2);
      return $$2;
   }

   ItemInteractionResult interact(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, ItemStack pStack);

   static void bootStrap() {
      Map<Item, CauldronInteraction> $$0 = EMPTY.map();
      addDefaultInteractions($$0);
      $$0.put(Items.POTION, (p_175732_, p_175733_, p_175734_, p_175735_, p_175736_, p_175737_) -> {
         PotionContents $$6 = p_175737_.get(DataComponents.POTION_CONTENTS);
         if ($$6 != null && $$6.is(Potions.WATER)) {
            if (!p_175733_.isClientSide) {
               Item $$7 = p_175737_.getItem();
               p_175735_.setItemInHand(p_175736_, ItemUtils.createFilledResult(p_175737_, p_175735_, new ItemStack(Items.GLASS_BOTTLE)));
               p_175735_.awardStat(Stats.USE_CAULDRON);
               p_175735_.awardStat(Stats.ITEM_USED.get($$7));
               p_175733_.setBlockAndUpdate(p_175734_, Blocks.WATER_CAULDRON.defaultBlockState());
               p_175733_.playSound(null, p_175734_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
               p_175733_.gameEvent(null, GameEvent.FLUID_PLACE, p_175734_);
            }

            return ItemInteractionResult.sidedSuccess(p_175733_.isClientSide);
         } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      });
      Map<Item, CauldronInteraction> $$1 = WATER.map();
      addDefaultInteractions($$1);
      $$1.put(
         Items.BUCKET,
         (p_325728_, p_325729_, p_325730_, p_325731_, p_325732_, p_325733_) -> fillBucket(
               p_325728_,
               p_325729_,
               p_325730_,
               p_325731_,
               p_325732_,
               p_325733_,
               new ItemStack(Items.WATER_BUCKET),
               p_175660_ -> p_175660_.getValue(LayeredCauldronBlock.LEVEL) == 3,
               SoundEvents.BUCKET_FILL
            )
      );
      $$1.put(Items.GLASS_BOTTLE, (p_325758_, p_325759_, p_325760_, p_325761_, p_325762_, p_325763_) -> {
         if (!p_325759_.isClientSide) {
            Item $$6 = p_325763_.getItem();
            p_325761_.setItemInHand(p_325762_, ItemUtils.createFilledResult(p_325763_, p_325761_, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
            p_325761_.awardStat(Stats.USE_CAULDRON);
            p_325761_.awardStat(Stats.ITEM_USED.get($$6));
            LayeredCauldronBlock.lowerFillLevel(p_325758_, p_325759_, p_325760_);
            p_325759_.playSound(null, p_325760_, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_325759_.gameEvent(null, GameEvent.FLUID_PICKUP, p_325760_);
         }

         return ItemInteractionResult.sidedSuccess(p_325759_.isClientSide);
      });
      $$1.put(Items.POTION, (p_175704_, p_175705_, p_175706_, p_175707_, p_175708_, p_175709_) -> {
         if (p_175704_.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else {
            PotionContents $$6 = p_175709_.get(DataComponents.POTION_CONTENTS);
            if ($$6 != null && $$6.is(Potions.WATER)) {
               if (!p_175705_.isClientSide) {
                  p_175707_.setItemInHand(p_175708_, ItemUtils.createFilledResult(p_175709_, p_175707_, new ItemStack(Items.GLASS_BOTTLE)));
                  p_175707_.awardStat(Stats.USE_CAULDRON);
                  p_175707_.awardStat(Stats.ITEM_USED.get(p_175709_.getItem()));
                  p_175705_.setBlockAndUpdate(p_175706_, p_175704_.cycle(LayeredCauldronBlock.LEVEL));
                  p_175705_.playSound(null, p_175706_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                  p_175705_.gameEvent(null, GameEvent.FLUID_PLACE, p_175706_);
               }

               return ItemInteractionResult.sidedSuccess(p_175705_.isClientSide);
            } else {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
         }
      });
      $$1.put(Items.LEATHER_BOOTS, DYED_ITEM);
      $$1.put(Items.LEATHER_LEGGINGS, DYED_ITEM);
      $$1.put(Items.LEATHER_CHESTPLATE, DYED_ITEM);
      $$1.put(Items.LEATHER_HELMET, DYED_ITEM);
      $$1.put(Items.LEATHER_HORSE_ARMOR, DYED_ITEM);
      $$1.put(Items.WOLF_ARMOR, DYED_ITEM);
      $$1.put(Items.WHITE_BANNER, BANNER);
      $$1.put(Items.GRAY_BANNER, BANNER);
      $$1.put(Items.BLACK_BANNER, BANNER);
      $$1.put(Items.BLUE_BANNER, BANNER);
      $$1.put(Items.BROWN_BANNER, BANNER);
      $$1.put(Items.CYAN_BANNER, BANNER);
      $$1.put(Items.GREEN_BANNER, BANNER);
      $$1.put(Items.LIGHT_BLUE_BANNER, BANNER);
      $$1.put(Items.LIGHT_GRAY_BANNER, BANNER);
      $$1.put(Items.LIME_BANNER, BANNER);
      $$1.put(Items.MAGENTA_BANNER, BANNER);
      $$1.put(Items.ORANGE_BANNER, BANNER);
      $$1.put(Items.PINK_BANNER, BANNER);
      $$1.put(Items.PURPLE_BANNER, BANNER);
      $$1.put(Items.RED_BANNER, BANNER);
      $$1.put(Items.YELLOW_BANNER, BANNER);
      $$1.put(Items.WHITE_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.GRAY_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.BLACK_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.BLUE_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.BROWN_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.CYAN_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.GREEN_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.LIGHT_BLUE_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.LIGHT_GRAY_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.LIME_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.MAGENTA_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.ORANGE_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.PINK_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.PURPLE_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.RED_SHULKER_BOX, SHULKER_BOX);
      $$1.put(Items.YELLOW_SHULKER_BOX, SHULKER_BOX);
      Map<Item, CauldronInteraction> $$2 = LAVA.map();
      $$2.put(
         Items.BUCKET,
         (p_325734_, p_325735_, p_325736_, p_325737_, p_325738_, p_325739_) -> fillBucket(
               p_325734_,
               p_325735_,
               p_325736_,
               p_325737_,
               p_325738_,
               p_325739_,
               new ItemStack(Items.LAVA_BUCKET),
               p_175651_ -> true,
               SoundEvents.BUCKET_FILL_LAVA
            )
      );
      addDefaultInteractions($$2);
      Map<Item, CauldronInteraction> $$3 = POWDER_SNOW.map();
      $$3.put(
         Items.BUCKET,
         (p_325740_, p_325741_, p_325742_, p_325743_, p_325744_, p_325745_) -> fillBucket(
               p_325740_,
               p_325741_,
               p_325742_,
               p_325743_,
               p_325744_,
               p_325745_,
               new ItemStack(Items.POWDER_SNOW_BUCKET),
               p_175627_ -> p_175627_.getValue(LayeredCauldronBlock.LEVEL) == 3,
               SoundEvents.BUCKET_FILL_POWDER_SNOW
            )
      );
      addDefaultInteractions($$3);
   }

   static void addDefaultInteractions(Map<Item, CauldronInteraction> pInteractionsMap) {
      pInteractionsMap.put(Items.LAVA_BUCKET, FILL_LAVA);
      pInteractionsMap.put(Items.WATER_BUCKET, FILL_WATER);
      pInteractionsMap.put(Items.POWDER_SNOW_BUCKET, FILL_POWDER_SNOW);
   }

   static ItemInteractionResult fillBucket(
      BlockState pState,
      Level pLevel,
      BlockPos pPos,
      Player pPlayer,
      InteractionHand pHand,
      ItemStack pEmptyStack,
      ItemStack pFilledStack,
      Predicate<BlockState> pStatePredicate,
      SoundEvent pFillSound
   ) {
      if (!pStatePredicate.test(pState)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         if (!pLevel.isClientSide) {
            Item $$9 = pEmptyStack.getItem();
            pPlayer.setItemInHand(pHand, ItemUtils.createFilledResult(pEmptyStack, pPlayer, pFilledStack));
            pPlayer.awardStat(Stats.USE_CAULDRON);
            pPlayer.awardStat(Stats.ITEM_USED.get($$9));
            pLevel.setBlockAndUpdate(pPos, Blocks.CAULDRON.defaultBlockState());
            pLevel.playSound(null, pPos, pFillSound, SoundSource.BLOCKS, 1.0F, 1.0F);
            pLevel.gameEvent(null, GameEvent.FLUID_PICKUP, pPos);
         }

         return ItemInteractionResult.sidedSuccess(pLevel.isClientSide);
      }
   }

   static ItemInteractionResult emptyBucket(
      Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, ItemStack pFilledStack, BlockState pState, SoundEvent pEmptySound
   ) {
      if (!pLevel.isClientSide) {
         Item $$7 = pFilledStack.getItem();
         pPlayer.setItemInHand(pHand, ItemUtils.createFilledResult(pFilledStack, pPlayer, new ItemStack(Items.BUCKET)));
         pPlayer.awardStat(Stats.FILL_CAULDRON);
         pPlayer.awardStat(Stats.ITEM_USED.get($$7));
         pLevel.setBlockAndUpdate(pPos, pState);
         pLevel.playSound(null, pPos, pEmptySound, SoundSource.BLOCKS, 1.0F, 1.0F);
         pLevel.gameEvent(null, GameEvent.FLUID_PLACE, pPos);
      }

      return ItemInteractionResult.sidedSuccess(pLevel.isClientSide);
   }

   public static record InteractionMap(String name, Map<Item, CauldronInteraction> map) {
   }
}
