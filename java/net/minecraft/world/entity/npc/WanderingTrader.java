package net.minecraft.world.entity.npc;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.UseItemGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class WanderingTrader extends AbstractVillager {
   private static final int NUMBER_OF_TRADE_OFFERS = 5;
   @Nullable
   private BlockPos wanderTarget;
   private int despawnDelay;

   public WanderingTrader(EntityType<? extends WanderingTrader> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector
         .addGoal(
            0,
            new UseItemGoal<>(
               this,
               PotionContents.createItemStack(Items.POTION, Potions.INVISIBILITY),
               SoundEvents.WANDERING_TRADER_DISAPPEARED,
               p_341477_ -> this.level().isNight() && !p_341477_.isInvisible()
            )
         );
      this.goalSelector
         .addGoal(
            0,
            new UseItemGoal<>(
               this, new ItemStack(Items.MILK_BUCKET), SoundEvents.WANDERING_TRADER_REAPPEARED, p_341478_ -> this.level().isDay() && p_341478_.isInvisible()
            )
         );
      this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Evoker.class, 12.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vindicator.class, 8.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vex.class, 8.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Pillager.class, 15.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Illusioner.class, 12.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zoglin.class, 10.0F, 0.5, 0.5));
      this.goalSelector.addGoal(1, new PanicGoal(this, 0.5));
      this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
      this.goalSelector.addGoal(2, new WanderingTrader.WanderToPositionGoal(this, 2.0, 0.35));
      this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
      this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
      this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
   }

   @Nullable
   @Override
   public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
      return null;
   }

   @Override
   public boolean showProgressBar() {
      return false;
   }

   @Override
   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack $$2 = pPlayer.getItemInHand(pHand);
      if (!$$2.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
         if (pHand == InteractionHand.MAIN_HAND) {
            pPlayer.awardStat(Stats.TALKED_TO_VILLAGER);
         }

         if (!this.level().isClientSide) {
            if (this.getOffers().isEmpty()) {
               return InteractionResult.CONSUME;
            }

            this.setTradingPlayer(pPlayer);
            this.openTradingScreen(pPlayer, this.getDisplayName(), 1);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         return super.mobInteract(pPlayer, pHand);
      }
   }

   @Override
   protected void updateTrades() {
      if (this.level().enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
         this.experimentalUpdateTrades();
      } else {
         VillagerTrades.ItemListing[] $$0 = (VillagerTrades.ItemListing[])VillagerTrades.WANDERING_TRADER_TRADES.get(1);
         VillagerTrades.ItemListing[] $$1 = (VillagerTrades.ItemListing[])VillagerTrades.WANDERING_TRADER_TRADES.get(2);
         if ($$0 != null && $$1 != null) {
            MerchantOffers $$2 = this.getOffers();
            this.addOffersFromItemListings($$2, $$0, 5);
            int $$3 = this.random.nextInt($$1.length);
            VillagerTrades.ItemListing $$4 = $$1[$$3];
            MerchantOffer $$5 = $$4.getOffer(this, this.random);
            if ($$5 != null) {
               $$2.add($$5);
            }
         }
      }
   }

   private void experimentalUpdateTrades() {
      MerchantOffers $$0 = this.getOffers();

      for (Pair<VillagerTrades.ItemListing[], Integer> $$1 : VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES) {
         VillagerTrades.ItemListing[] $$2 = (VillagerTrades.ItemListing[])$$1.getLeft();
         this.addOffersFromItemListings($$0, $$2, (Integer)$$1.getRight());
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("DespawnDelay", this.despawnDelay);
      if (this.wanderTarget != null) {
         pCompound.put("wander_target", NbtUtils.writeBlockPos(this.wanderTarget));
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      if (pCompound.contains("DespawnDelay", 99)) {
         this.despawnDelay = pCompound.getInt("DespawnDelay");
      }

      NbtUtils.readBlockPos(pCompound, "wander_target").ifPresent(p_327049_ -> this.wanderTarget = p_327049_);
      this.setAge(Math.max(0, this.getAge()));
   }

   @Override
   public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
      return false;
   }

   @Override
   protected void rewardTradeXp(MerchantOffer pOffer) {
      if (pOffer.shouldRewardExp()) {
         int $$1 = 3 + this.random.nextInt(4);
         this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), $$1));
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return this.isTrading() ? SoundEvents.WANDERING_TRADER_TRADE : SoundEvents.WANDERING_TRADER_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.WANDERING_TRADER_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.WANDERING_TRADER_DEATH;
   }

   @Override
   protected SoundEvent getDrinkingSound(ItemStack pStack) {
      return pStack.is(Items.MILK_BUCKET) ? SoundEvents.WANDERING_TRADER_DRINK_MILK : SoundEvents.WANDERING_TRADER_DRINK_POTION;
   }

   @Override
   protected SoundEvent getTradeUpdatedSound(boolean pGetYesSound) {
      return pGetYesSound ? SoundEvents.WANDERING_TRADER_YES : SoundEvents.WANDERING_TRADER_NO;
   }

   @Override
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.WANDERING_TRADER_YES;
   }

   public void setDespawnDelay(int pDespawnDelay) {
      this.despawnDelay = pDespawnDelay;
   }

   public int getDespawnDelay() {
      return this.despawnDelay;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide) {
         this.maybeDespawn();
      }
   }

   private void maybeDespawn() {
      if (this.despawnDelay > 0 && !this.isTrading() && --this.despawnDelay == 0) {
         this.discard();
      }
   }

   public void setWanderTarget(@Nullable BlockPos pWanderTarget) {
      this.wanderTarget = pWanderTarget;
   }

   @Nullable
   BlockPos getWanderTarget() {
      return this.wanderTarget;
   }

   class WanderToPositionGoal extends Goal {
      final WanderingTrader trader;
      final double stopDistance;
      final double speedModifier;

      WanderToPositionGoal(final WanderingTrader pTrader, final double pStopDistance, final double pSpeedModifier) {
         this.trader = pTrader;
         this.stopDistance = pStopDistance;
         this.speedModifier = pSpeedModifier;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      @Override
      public void stop() {
         this.trader.setWanderTarget(null);
         WanderingTrader.this.navigation.stop();
      }

      @Override
      public boolean canUse() {
         BlockPos $$0 = this.trader.getWanderTarget();
         return $$0 != null && this.isTooFarAway($$0, this.stopDistance);
      }

      @Override
      public void tick() {
         BlockPos $$0 = this.trader.getWanderTarget();
         if ($$0 != null && WanderingTrader.this.navigation.isDone()) {
            if (this.isTooFarAway($$0, 10.0)) {
               Vec3 $$1 = new Vec3((double)$$0.getX() - this.trader.getX(), (double)$$0.getY() - this.trader.getY(), (double)$$0.getZ() - this.trader.getZ())
                  .normalize();
               Vec3 $$2 = $$1.scale(10.0).add(this.trader.getX(), this.trader.getY(), this.trader.getZ());
               WanderingTrader.this.navigation.moveTo($$2.x, $$2.y, $$2.z, this.speedModifier);
            } else {
               WanderingTrader.this.navigation.moveTo((double)$$0.getX(), (double)$$0.getY(), (double)$$0.getZ(), this.speedModifier);
            }
         }
      }

      private boolean isTooFarAway(BlockPos pPos, double pDistance) {
         return !pPos.closerToCenterThan(this.trader.position(), pDistance);
      }
   }
}
