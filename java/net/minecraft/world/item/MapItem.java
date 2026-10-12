package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapItem extends ComplexItem {
   public static final int IMAGE_WIDTH = 128;
   public static final int IMAGE_HEIGHT = 128;

   public MapItem(Item.Properties pProperties) {
      super(pProperties);
   }

   public static ItemStack create(Level pLevel, int pLevelX, int pLevelZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking) {
      ItemStack $$6 = new ItemStack(Items.FILLED_MAP);
      MapId $$7 = createNewSavedData(pLevel, pLevelX, pLevelZ, pScale, pTrackingPosition, pUnlimitedTracking, pLevel.dimension());
      $$6.set(DataComponents.MAP_ID, $$7);
      return $$6;
   }

   @Nullable
   public static MapItemSavedData getSavedData(@Nullable MapId pMapId, Level pLevel) {
      return pMapId == null ? null : pLevel.getMapData(pMapId);
   }

   @Nullable
   public static MapItemSavedData getSavedData(ItemStack pStack, Level pLevel) {
      MapId $$2 = pStack.get(DataComponents.MAP_ID);
      return getSavedData($$2, pLevel);
   }

   private static MapId createNewSavedData(
      Level pLevel, int pX, int pZ, int pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, ResourceKey<Level> pDimension
   ) {
      MapItemSavedData $$7 = MapItemSavedData.createFresh((double)pX, (double)pZ, (byte)pScale, pTrackingPosition, pUnlimitedTracking, pDimension);
      MapId $$8 = pLevel.getFreeMapId();
      pLevel.setMapData($$8, $$7);
      return $$8;
   }

   public void update(Level pLevel, Entity pViewer, MapItemSavedData pData) {
      if (pLevel.dimension() == pData.dimension && pViewer instanceof Player) {
         int $$3 = 1 << pData.scale;
         int $$4 = pData.centerX;
         int $$5 = pData.centerZ;
         int $$6 = Mth.floor(pViewer.getX() - (double)$$4) / $$3 + 64;
         int $$7 = Mth.floor(pViewer.getZ() - (double)$$5) / $$3 + 64;
         int $$8 = 128 / $$3;
         if (pLevel.dimensionType().hasCeiling()) {
            $$8 /= 2;
         }

         MapItemSavedData.HoldingPlayer $$9 = pData.getHoldingPlayer((Player)pViewer);
         $$9.step++;
         BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos $$11 = new BlockPos.MutableBlockPos();
         boolean $$12 = false;

         for (int $$13 = $$6 - $$8 + 1; $$13 < $$6 + $$8; $$13++) {
            if (($$13 & 15) == ($$9.step & 15) || $$12) {
               $$12 = false;
               double $$14 = 0.0;

               for (int $$15 = $$7 - $$8 - 1; $$15 < $$7 + $$8; $$15++) {
                  if ($$13 >= 0 && $$15 >= -1 && $$13 < 128 && $$15 < 128) {
                     int $$16 = Mth.square($$13 - $$6) + Mth.square($$15 - $$7);
                     boolean $$17 = $$16 > ($$8 - 2) * ($$8 - 2);
                     int $$18 = ($$4 / $$3 + $$13 - 64) * $$3;
                     int $$19 = ($$5 / $$3 + $$15 - 64) * $$3;
                     Multiset<MapColor> $$20 = LinkedHashMultiset.create();
                     LevelChunk $$21 = pLevel.getChunk(SectionPos.blockToSectionCoord($$18), SectionPos.blockToSectionCoord($$19));
                     if (!$$21.isEmpty()) {
                        int $$22 = 0;
                        double $$23 = 0.0;
                        if (pLevel.dimensionType().hasCeiling()) {
                           int $$24 = $$18 + $$19 * 231871;
                           $$24 = $$24 * $$24 * 31287121 + $$24 * 11;
                           if (($$24 >> 20 & 1) == 0) {
                              $$20.add(Blocks.DIRT.defaultBlockState().getMapColor(pLevel, BlockPos.ZERO), 10);
                           } else {
                              $$20.add(Blocks.STONE.defaultBlockState().getMapColor(pLevel, BlockPos.ZERO), 100);
                           }

                           $$23 = 100.0;
                        } else {
                           for (int $$25 = 0; $$25 < $$3; $$25++) {
                              for (int $$26 = 0; $$26 < $$3; $$26++) {
                                 $$10.set($$18 + $$25, 0, $$19 + $$26);
                                 int $$27 = $$21.getHeight(Heightmap.Types.WORLD_SURFACE, $$10.getX(), $$10.getZ()) + 1;
                                 BlockState $$31;
                                 if ($$27 <= pLevel.getMinBuildHeight() + 1) {
                                    $$31 = Blocks.BEDROCK.defaultBlockState();
                                 } else {
                                    do {
                                       $$10.setY(--$$27);
                                       $$31 = $$21.getBlockState($$10);
                                    } while ($$31.getMapColor(pLevel, $$10) == MapColor.NONE && $$27 > pLevel.getMinBuildHeight());

                                    if ($$27 > pLevel.getMinBuildHeight() && !$$31.getFluidState().isEmpty()) {
                                       int $$29 = $$27 - 1;
                                       $$11.set($$10);

                                       BlockState $$30;
                                       do {
                                          $$11.setY($$29--);
                                          $$30 = $$21.getBlockState($$11);
                                          $$22++;
                                       } while ($$29 > pLevel.getMinBuildHeight() && !$$30.getFluidState().isEmpty());

                                       $$31 = this.getCorrectStateForFluidBlock(pLevel, $$31, $$10);
                                    }
                                 }

                                 pData.checkBanners(pLevel, $$10.getX(), $$10.getZ());
                                 $$23 += (double)$$27 / (double)($$3 * $$3);
                                 $$20.add($$31.getMapColor(pLevel, $$10));
                              }
                           }
                        }

                        $$22 /= $$3 * $$3;
                        MapColor $$32 = (MapColor)Iterables.getFirst(Multisets.copyHighestCountFirst($$20), MapColor.NONE);
                        MapColor.Brightness $$34;
                        if ($$32 == MapColor.WATER) {
                           double $$33 = (double)$$22 * 0.1 + (double)($$13 + $$15 & 1) * 0.2;
                           if ($$33 < 0.5) {
                              $$34 = MapColor.Brightness.HIGH;
                           } else if ($$33 > 0.9) {
                              $$34 = MapColor.Brightness.LOW;
                           } else {
                              $$34 = MapColor.Brightness.NORMAL;
                           }
                        } else {
                           double $$37 = ($$23 - $$14) * 4.0 / (double)($$3 + 4) + ((double)($$13 + $$15 & 1) - 0.5) * 0.4;
                           if ($$37 > 0.6) {
                              $$34 = MapColor.Brightness.HIGH;
                           } else if ($$37 < -0.6) {
                              $$34 = MapColor.Brightness.LOW;
                           } else {
                              $$34 = MapColor.Brightness.NORMAL;
                           }
                        }

                        $$14 = $$23;
                        if ($$15 >= 0 && $$16 < $$8 * $$8 && (!$$17 || ($$13 + $$15 & 1) != 0)) {
                           $$12 |= pData.updateColor($$13, $$15, $$32.getPackedId($$34));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private BlockState getCorrectStateForFluidBlock(Level pLevel, BlockState pState, BlockPos pPos) {
      FluidState $$3 = pState.getFluidState();
      return !$$3.isEmpty() && !pState.isFaceSturdy(pLevel, pPos, Direction.UP) ? $$3.createLegacyBlock() : pState;
   }

   private static boolean isBiomeWatery(boolean[] pWateryMap, int pXSample, int pZSample) {
      return pWateryMap[pZSample * 128 + pXSample];
   }

   public static void renderBiomePreviewMap(ServerLevel pServerLevel, ItemStack pStack) {
      MapItemSavedData $$2 = getSavedData(pStack, pServerLevel);
      if ($$2 != null) {
         if (pServerLevel.dimension() == $$2.dimension) {
            int $$3 = 1 << $$2.scale;
            int $$4 = $$2.centerX;
            int $$5 = $$2.centerZ;
            boolean[] $$6 = new boolean[16384];
            int $$7 = $$4 / $$3 - 64;
            int $$8 = $$5 / $$3 - 64;
            BlockPos.MutableBlockPos $$9 = new BlockPos.MutableBlockPos();

            for (int $$10 = 0; $$10 < 128; $$10++) {
               for (int $$11 = 0; $$11 < 128; $$11++) {
                  Holder<Biome> $$12 = pServerLevel.getBiome($$9.set(($$7 + $$11) * $$3, 0, ($$8 + $$10) * $$3));
                  $$6[$$10 * 128 + $$11] = $$12.is(BiomeTags.WATER_ON_MAP_OUTLINES);
               }
            }

            for (int $$13 = 1; $$13 < 127; $$13++) {
               for (int $$14 = 1; $$14 < 127; $$14++) {
                  int $$15 = 0;

                  for (int $$16 = -1; $$16 < 2; $$16++) {
                     for (int $$17 = -1; $$17 < 2; $$17++) {
                        if (($$16 != 0 || $$17 != 0) && isBiomeWatery($$6, $$13 + $$16, $$14 + $$17)) {
                           $$15++;
                        }
                     }
                  }

                  MapColor.Brightness $$18 = MapColor.Brightness.LOWEST;
                  MapColor $$19 = MapColor.NONE;
                  if (isBiomeWatery($$6, $$13, $$14)) {
                     $$19 = MapColor.COLOR_ORANGE;
                     if ($$15 > 7 && $$14 % 2 == 0) {
                        switch (($$13 + (int)(Mth.sin((float)$$14 + 0.0F) * 7.0F)) / 8 % 5) {
                           case 0:
                           case 4:
                              $$18 = MapColor.Brightness.LOW;
                              break;
                           case 1:
                           case 3:
                              $$18 = MapColor.Brightness.NORMAL;
                              break;
                           case 2:
                              $$18 = MapColor.Brightness.HIGH;
                        }
                     } else if ($$15 > 7) {
                        $$19 = MapColor.NONE;
                     } else if ($$15 > 5) {
                        $$18 = MapColor.Brightness.NORMAL;
                     } else if ($$15 > 3) {
                        $$18 = MapColor.Brightness.LOW;
                     } else if ($$15 > 1) {
                        $$18 = MapColor.Brightness.LOW;
                     }
                  } else if ($$15 > 0) {
                     $$19 = MapColor.COLOR_BROWN;
                     if ($$15 > 3) {
                        $$18 = MapColor.Brightness.NORMAL;
                     } else {
                        $$18 = MapColor.Brightness.LOWEST;
                     }
                  }

                  if ($$19 != MapColor.NONE) {
                     $$2.setColor($$13, $$14, $$19.getPackedId($$18));
                  }
               }
            }
         }
      }
   }

   @Override
   public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pItemSlot, boolean pIsSelected) {
      if (!pLevel.isClientSide) {
         MapItemSavedData $$5 = getSavedData(pStack, pLevel);
         if ($$5 != null) {
            if (pEntity instanceof Player $$6) {
               $$5.tickCarriedBy($$6, pStack);
            }

            if (!$$5.locked && (pIsSelected || pEntity instanceof Player && ((Player)pEntity).getOffhandItem() == pStack)) {
               this.update(pLevel, pEntity, $$5);
            }
         }
      }
   }

   @Nullable
   @Override
   public Packet<?> getUpdatePacket(ItemStack pStack, Level pLevel, Player pPlayer) {
      MapId $$3 = pStack.get(DataComponents.MAP_ID);
      MapItemSavedData $$4 = getSavedData($$3, pLevel);
      return $$4 != null ? $$4.getUpdatePacket($$3, pPlayer) : null;
   }

   @Override
   public void onCraftedPostProcess(ItemStack pStack, Level pLevel) {
      MapPostProcessing $$2 = pStack.remove(DataComponents.MAP_POST_PROCESSING);
      if ($$2 != null) {
         switch ($$2) {
            case LOCK:
               lockMap(pLevel, pStack);
               break;
            case SCALE:
               scaleMap(pStack, pLevel);
         }
      }
   }

   private static void scaleMap(ItemStack pStack, Level pLevel) {
      MapItemSavedData $$2 = getSavedData(pStack, pLevel);
      if ($$2 != null) {
         MapId $$3 = pLevel.getFreeMapId();
         pLevel.setMapData($$3, $$2.scaled());
         pStack.set(DataComponents.MAP_ID, $$3);
      }
   }

   public static void lockMap(Level pLevel, ItemStack pStack) {
      MapItemSavedData $$2 = getSavedData(pStack, pLevel);
      if ($$2 != null) {
         MapId $$3 = pLevel.getFreeMapId();
         MapItemSavedData $$4 = $$2.locked();
         pLevel.setMapData($$3, $$4);
         pStack.set(DataComponents.MAP_ID, $$3);
      }
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      MapId $$4 = pStack.get(DataComponents.MAP_ID);
      MapItemSavedData $$5 = $$4 != null ? pContext.mapData($$4) : null;
      MapPostProcessing $$6 = pStack.get(DataComponents.MAP_POST_PROCESSING);
      if ($$5 != null && ($$5.locked || $$6 == MapPostProcessing.LOCK)) {
         pTooltipComponents.add(Component.translatable("filled_map.locked", $$4.id()).withStyle(ChatFormatting.GRAY));
      }

      if (pTooltipFlag.isAdvanced()) {
         if ($$5 != null) {
            if ($$6 == null) {
               pTooltipComponents.add(getTooltipForId($$4));
            }

            int $$7 = $$6 == MapPostProcessing.SCALE ? 1 : 0;
            int $$8 = Math.min($$5.scale + $$7, 4);
            pTooltipComponents.add(Component.translatable("filled_map.scale", 1 << $$8).withStyle(ChatFormatting.GRAY));
            pTooltipComponents.add(Component.translatable("filled_map.level", $$8, 4).withStyle(ChatFormatting.GRAY));
         } else {
            pTooltipComponents.add(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));
         }
      }
   }

   public static Component getTooltipForId(MapId pMapId) {
      return Component.translatable("filled_map.id", pMapId.id()).withStyle(ChatFormatting.GRAY);
   }

   @Override
   public InteractionResult useOn(UseOnContext pContext) {
      BlockState $$1 = pContext.getLevel().getBlockState(pContext.getClickedPos());
      if ($$1.is(BlockTags.BANNERS)) {
         if (!pContext.getLevel().isClientSide) {
            MapItemSavedData $$2 = getSavedData(pContext.getItemInHand(), pContext.getLevel());
            if ($$2 != null && !$$2.toggleBanner(pContext.getLevel(), pContext.getClickedPos())) {
               return InteractionResult.FAIL;
            }
         }

         return InteractionResult.sidedSuccess(pContext.getLevel().isClientSide);
      } else {
         return super.useOn(pContext);
      }
   }
}
