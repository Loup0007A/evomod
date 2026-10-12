package net.minecraft.world.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;

public class AreaEffectCloud extends Entity implements TraceableEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int TIME_BETWEEN_APPLICATIONS = 5;
   private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_WAITING = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<ParticleOptions> DATA_PARTICLE = SynchedEntityData.defineId(AreaEffectCloud.class, EntityDataSerializers.PARTICLE);
   private static final float MAX_RADIUS = 32.0F;
   private static final float MINIMAL_RADIUS = 0.5F;
   private static final float DEFAULT_RADIUS = 3.0F;
   public static final float DEFAULT_WIDTH = 6.0F;
   public static final float HEIGHT = 0.5F;
   private PotionContents potionContents = PotionContents.EMPTY;
   private final Map<Entity, Integer> victims = Maps.newHashMap();
   private int duration = 600;
   private int waitTime = 20;
   private int reapplicationDelay = 20;
   private int durationOnUse;
   private float radiusOnUse;
   private float radiusPerTick;
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUUID;

   public AreaEffectCloud(EntityType<? extends AreaEffectCloud> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.noPhysics = true;
   }

   public AreaEffectCloud(Level pLevel, double pX, double pY, double pZ) {
      this(EntityType.AREA_EFFECT_CLOUD, pLevel);
      this.setPos(pX, pY, pZ);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
      pBuilder.define(DATA_RADIUS, 3.0F);
      pBuilder.define(DATA_WAITING, false);
      pBuilder.define(DATA_PARTICLE, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1));
   }

   public void setRadius(float pRadius) {
      if (!this.level().isClientSide) {
         this.getEntityData().set(DATA_RADIUS, Mth.clamp(pRadius, 0.0F, 32.0F));
      }
   }

   @Override
   public void refreshDimensions() {
      double $$0 = this.getX();
      double $$1 = this.getY();
      double $$2 = this.getZ();
      super.refreshDimensions();
      this.setPos($$0, $$1, $$2);
   }

   public float getRadius() {
      return this.getEntityData().get(DATA_RADIUS);
   }

   public void setPotionContents(PotionContents p_332440_) {
      this.potionContents = p_332440_;
      this.updateColor();
   }

   private void updateColor() {
      ParticleOptions $$0 = this.entityData.get(DATA_PARTICLE);
      if ($$0 instanceof ColorParticleOption $$1) {
         int $$2 = this.potionContents.equals(PotionContents.EMPTY) ? 0 : this.potionContents.getColor();
         this.entityData.set(DATA_PARTICLE, ColorParticleOption.create($$1.getType(), FastColor.ARGB32.opaque($$2)));
      }
   }

   public void addEffect(MobEffectInstance pEffectInstance) {
      this.setPotionContents(this.potionContents.withEffectAdded(pEffectInstance));
   }

   public ParticleOptions getParticle() {
      return this.getEntityData().get(DATA_PARTICLE);
   }

   public void setParticle(ParticleOptions p_19725_) {
      this.getEntityData().set(DATA_PARTICLE, p_19725_);
   }

   protected void setWaiting(boolean pWaiting) {
      this.getEntityData().set(DATA_WAITING, pWaiting);
   }

   public boolean isWaiting() {
      return this.getEntityData().get(DATA_WAITING);
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int pDuration) {
      this.duration = pDuration;
   }

   @Override
   public void tick() {
      super.tick();
      boolean $$0 = this.isWaiting();
      float $$1 = this.getRadius();
      if (this.level().isClientSide) {
         if ($$0 && this.random.nextBoolean()) {
            return;
         }

         ParticleOptions $$2 = this.getParticle();
         int $$3;
         float $$4;
         if ($$0) {
            $$3 = 2;
            $$4 = 0.2F;
         } else {
            $$3 = Mth.ceil((float) Math.PI * $$1 * $$1);
            $$4 = $$1;
         }

         for (int $$7 = 0; $$7 < $$3; $$7++) {
            float $$8 = this.random.nextFloat() * (float) (Math.PI * 2);
            float $$9 = Mth.sqrt(this.random.nextFloat()) * $$4;
            double $$10 = this.getX() + (double)(Mth.cos($$8) * $$9);
            double $$11 = this.getY();
            double $$12 = this.getZ() + (double)(Mth.sin($$8) * $$9);
            if ($$2.getType() == ParticleTypes.ENTITY_EFFECT) {
               if ($$0 && this.random.nextBoolean()) {
                  this.level().addAlwaysVisibleParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, -1), $$10, $$11, $$12, 0.0, 0.0, 0.0);
               } else {
                  this.level().addAlwaysVisibleParticle($$2, $$10, $$11, $$12, 0.0, 0.0, 0.0);
               }
            } else if ($$0) {
               this.level().addAlwaysVisibleParticle($$2, $$10, $$11, $$12, 0.0, 0.0, 0.0);
            } else {
               this.level()
                  .addAlwaysVisibleParticle($$2, $$10, $$11, $$12, (0.5 - this.random.nextDouble()) * 0.15, 0.01F, (0.5 - this.random.nextDouble()) * 0.15);
            }
         }
      } else {
         if (this.tickCount >= this.waitTime + this.duration) {
            this.discard();
            return;
         }

         boolean $$13 = this.tickCount < this.waitTime;
         if ($$0 != $$13) {
            this.setWaiting($$13);
         }

         if ($$13) {
            return;
         }

         if (this.radiusPerTick != 0.0F) {
            $$1 += this.radiusPerTick;
            if ($$1 < 0.5F) {
               this.discard();
               return;
            }

            this.setRadius($$1);
         }

         if (this.tickCount % 5 == 0) {
            this.victims.entrySet().removeIf(p_287380_ -> this.tickCount >= p_287380_.getValue());
            if (!this.potionContents.hasEffects()) {
               this.victims.clear();
            } else {
               List<MobEffectInstance> $$14 = Lists.newArrayList();
               if (this.potionContents.potion().isPresent()) {
                  for (MobEffectInstance $$15 : this.potionContents.potion().get().value().getEffects()) {
                     $$14.add(
                        new MobEffectInstance(
                           $$15.getEffect(), $$15.mapDuration(p_267926_ -> p_267926_ / 4), $$15.getAmplifier(), $$15.isAmbient(), $$15.isVisible()
                        )
                     );
                  }
               }

               $$14.addAll(this.potionContents.customEffects());
               List<LivingEntity> $$16 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
               if (!$$16.isEmpty()) {
                  for (LivingEntity $$17 : $$16) {
                     if (!this.victims.containsKey($$17) && $$17.isAffectedByPotions() && !$$14.stream().noneMatch($$17::canBeAffected)) {
                        double $$18 = $$17.getX() - this.getX();
                        double $$19 = $$17.getZ() - this.getZ();
                        double $$20 = $$18 * $$18 + $$19 * $$19;
                        if ($$20 <= (double)($$1 * $$1)) {
                           this.victims.put($$17, this.tickCount + this.reapplicationDelay);

                           for (MobEffectInstance $$21 : $$14) {
                              if ($$21.getEffect().value().isInstantenous()) {
                                 $$21.getEffect().value().applyInstantenousEffect(this, this.getOwner(), $$17, $$21.getAmplifier(), 0.5);
                              } else {
                                 $$17.addEffect(new MobEffectInstance($$21), this);
                              }
                           }

                           if (this.radiusOnUse != 0.0F) {
                              $$1 += this.radiusOnUse;
                              if ($$1 < 0.5F) {
                                 this.discard();
                                 return;
                              }

                              this.setRadius($$1);
                           }

                           if (this.durationOnUse != 0) {
                              this.duration = this.duration + this.durationOnUse;
                              if (this.duration <= 0) {
                                 this.discard();
                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public float getRadiusOnUse() {
      return this.radiusOnUse;
   }

   public void setRadiusOnUse(float pRadiusOnUse) {
      this.radiusOnUse = pRadiusOnUse;
   }

   public float getRadiusPerTick() {
      return this.radiusPerTick;
   }

   public void setRadiusPerTick(float pRadiusPerTick) {
      this.radiusPerTick = pRadiusPerTick;
   }

   public int getDurationOnUse() {
      return this.durationOnUse;
   }

   public void setDurationOnUse(int pDurationOnUse) {
      this.durationOnUse = pDurationOnUse;
   }

   public int getWaitTime() {
      return this.waitTime;
   }

   public void setWaitTime(int pWaitTime) {
      this.waitTime = pWaitTime;
   }

   public void setOwner(@Nullable LivingEntity pOwner) {
      this.owner = pOwner;
      this.ownerUUID = pOwner == null ? null : pOwner.getUUID();
   }

   @Nullable
   public LivingEntity getOwner() {
      if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
         Entity $$0 = ((ServerLevel)this.level()).getEntity(this.ownerUUID);
         if ($$0 instanceof LivingEntity) {
            this.owner = (LivingEntity)$$0;
         }
      }

      return this.owner;
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag pCompound) {
      this.tickCount = pCompound.getInt("Age");
      this.duration = pCompound.getInt("Duration");
      this.waitTime = pCompound.getInt("WaitTime");
      this.reapplicationDelay = pCompound.getInt("ReapplicationDelay");
      this.durationOnUse = pCompound.getInt("DurationOnUse");
      this.radiusOnUse = pCompound.getFloat("RadiusOnUse");
      this.radiusPerTick = pCompound.getFloat("RadiusPerTick");
      this.setRadius(pCompound.getFloat("Radius"));
      if (pCompound.hasUUID("Owner")) {
         this.ownerUUID = pCompound.getUUID("Owner");
      }

      RegistryOps<Tag> $$1 = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      if (pCompound.contains("Particle", 10)) {
         ParticleTypes.CODEC
            .parse($$1, pCompound.get("Particle"))
            .resultOrPartial(p_326760_ -> LOGGER.warn("Failed to parse area effect cloud particle options: '{}'", p_326760_))
            .ifPresent(this::setParticle);
      }

      if (pCompound.contains("potion_contents")) {
         PotionContents.CODEC
            .parse($$1, pCompound.get("potion_contents"))
            .resultOrPartial(p_326761_ -> LOGGER.warn("Failed to parse area effect cloud potions: '{}'", p_326761_))
            .ifPresent(this::setPotionContents);
      }
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag pCompound) {
      pCompound.putInt("Age", this.tickCount);
      pCompound.putInt("Duration", this.duration);
      pCompound.putInt("WaitTime", this.waitTime);
      pCompound.putInt("ReapplicationDelay", this.reapplicationDelay);
      pCompound.putInt("DurationOnUse", this.durationOnUse);
      pCompound.putFloat("RadiusOnUse", this.radiusOnUse);
      pCompound.putFloat("RadiusPerTick", this.radiusPerTick);
      pCompound.putFloat("Radius", this.getRadius());
      RegistryOps<Tag> $$1 = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
      pCompound.put("Particle", (Tag)ParticleTypes.CODEC.encodeStart($$1, this.getParticle()).getOrThrow());
      if (this.ownerUUID != null) {
         pCompound.putUUID("Owner", this.ownerUUID);
      }

      if (!this.potionContents.equals(PotionContents.EMPTY)) {
         Tag $$2 = (Tag)PotionContents.CODEC.encodeStart($$1, this.potionContents).getOrThrow();
         pCompound.put("potion_contents", $$2);
      }
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      if (DATA_RADIUS.equals(pKey)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(pKey);
   }

   @Override
   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @Override
   public EntityDimensions getDimensions(Pose pPose) {
      return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
   }
}
