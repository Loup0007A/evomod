package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class KnowledgeBookItem extends Item {
   private static final Logger LOGGER = LogUtils.getLogger();

   public KnowledgeBookItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      List<ResourceLocation> $$4 = $$3.getOrDefault(DataComponents.RECIPES, List.of());
      $$3.consume(1, pPlayer);
      if ($$4.isEmpty()) {
         return InteractionResultHolder.fail($$3);
      } else {
         if (!pLevel.isClientSide) {
            RecipeManager $$5 = pLevel.getServer().getRecipeManager();
            List<RecipeHolder<?>> $$6 = new ArrayList<>($$4.size());

            for (ResourceLocation $$7 : $$4) {
               Optional<RecipeHolder<?>> $$8 = $$5.byKey($$7);
               if (!$$8.isPresent()) {
                  LOGGER.error("Invalid recipe: {}", $$7);
                  return InteractionResultHolder.fail($$3);
               }

               $$6.add($$8.get());
            }

            pPlayer.awardRecipes($$6);
            pPlayer.awardStat(Stats.ITEM_USED.get(this));
         }

         return InteractionResultHolder.sidedSuccess($$3, pLevel.isClientSide());
      }
   }
}
