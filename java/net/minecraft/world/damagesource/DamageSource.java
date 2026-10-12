package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DamageSource {
   private final Holder<DamageType> type;
   @Nullable
   private final Entity causingEntity;
   @Nullable
   private final Entity directEntity;
   @Nullable
   private final Vec3 damageSourcePosition;

   @Override
   public String toString() {
      return "DamageSource (" + this.type().msgId() + ")";
   }

   public float getFoodExhaustion() {
      return this.type().exhaustion();
   }

   public boolean isDirect() {
      return this.causingEntity == this.directEntity;
   }

   public DamageSource(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity, @Nullable Vec3 pDamageSourcePosition) {
      this.type = pType;
      this.causingEntity = pCausingEntity;
      this.directEntity = pDirectEntity;
      this.damageSourcePosition = pDamageSourcePosition;
   }

   public DamageSource(Holder<DamageType> pType, @Nullable Entity pDirectEntity, @Nullable Entity pCausingEntity) {
      this(pType, pDirectEntity, pCausingEntity, null);
   }

   public DamageSource(Holder<DamageType> pType, Vec3 pDamageSourcePosition) {
      this(pType, null, null, pDamageSourcePosition);
   }

   public DamageSource(Holder<DamageType> pType, @Nullable Entity pEntity) {
      this(pType, pEntity, pEntity);
   }

   public DamageSource(Holder<DamageType> pType) {
      this(pType, null, null, null);
   }

   @Nullable
   public Entity getDirectEntity() {
      return this.directEntity;
   }

   @Nullable
   public Entity getEntity() {
      return this.causingEntity;
   }

   @Nullable
   public ItemStack getWeaponItem() {
      return this.directEntity != null ? this.directEntity.getWeaponItem() : null;
   }

   public Component getLocalizedDeathMessage(LivingEntity pLivingEntity) {
      String $$1 = "death.attack." + this.type().msgId();
      if (this.causingEntity == null && this.directEntity == null) {
         LivingEntity $$5 = pLivingEntity.getKillCredit();
         String $$6 = $$1 + ".player";
         return $$5 != null
            ? Component.translatable($$6, pLivingEntity.getDisplayName(), $$5.getDisplayName())
            : Component.translatable($$1, pLivingEntity.getDisplayName());
      } else {
         Component $$2 = this.causingEntity == null ? this.directEntity.getDisplayName() : this.causingEntity.getDisplayName();
         ItemStack $$4 = this.causingEntity instanceof LivingEntity $$3 ? $$3.getMainHandItem() : ItemStack.EMPTY;
         return !$$4.isEmpty() && $$4.has(DataComponents.CUSTOM_NAME)
            ? Component.translatable($$1 + ".item", pLivingEntity.getDisplayName(), $$2, $$4.getDisplayName())
            : Component.translatable($$1, pLivingEntity.getDisplayName(), $$2);
      }
   }

   public String getMsgId() {
      return this.type().msgId();
   }

   public boolean scalesWithDifficulty() {
      return switch (this.type().scaling()) {
         case NEVER -> false;
         case WHEN_CAUSED_BY_LIVING_NON_PLAYER -> this.causingEntity instanceof LivingEntity && !(this.causingEntity instanceof Player);
         case ALWAYS -> true;
      };
   }

   public boolean isCreativePlayer() {
      if (this.getEntity() instanceof Player $$0 && $$0.getAbilities().instabuild) {
         return true;
      }

      return false;
   }

   @Nullable
   public Vec3 getSourcePosition() {
      if (this.damageSourcePosition != null) {
         return this.damageSourcePosition;
      } else {
         return this.directEntity != null ? this.directEntity.position() : null;
      }
   }

   @Nullable
   public Vec3 sourcePositionRaw() {
      return this.damageSourcePosition;
   }

   public boolean is(TagKey<DamageType> pDamageTypeKey) {
      return this.type.is(pDamageTypeKey);
   }

   public boolean is(ResourceKey<DamageType> pDamageTypeKey) {
      return this.type.is(pDamageTypeKey);
   }

   public DamageType type() {
      return this.type.value();
   }

   public Holder<DamageType> typeHolder() {
      return this.type;
   }
}
