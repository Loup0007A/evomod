package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SummonEntityEffect(HolderSet<EntityType<?>> entityTypes, boolean joinTeam) implements EnchantmentEntityEffect {
   public static final MapCodec<SummonEntityEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_345460_ -> p_345460_.group(
               RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity").forGetter(SummonEntityEffect::entityTypes),
               Codec.BOOL.optionalFieldOf("join_team", false).forGetter(SummonEntityEffect::joinTeam)
            )
            .apply(p_345460_, SummonEntityEffect::new)
   );

   @Override
   public void apply(ServerLevel pLevel, int pEnchantmentLevel, EnchantedItemInUse pItem, Entity pEntity, Vec3 pOrigin) {
      BlockPos $$5 = BlockPos.containing(pOrigin);
      if (Level.isInSpawnableBounds($$5)) {
         Optional<Holder<EntityType<?>>> $$6 = this.entityTypes().getRandomElement(pLevel.getRandom());
         if (!$$6.isEmpty()) {
            Entity $$7 = $$6.get().value().spawn(pLevel, $$5, MobSpawnType.TRIGGERED);
            if ($$7 != null) {
               if ($$7 instanceof LightningBolt $$8 && pItem.owner() instanceof ServerPlayer $$9) {
                  $$8.setCause($$9);
               }

               if (this.joinTeam && pEntity.getTeam() != null) {
                  pLevel.getScoreboard().addPlayerToTeam($$7.getScoreboardName(), pEntity.getTeam());
               }

               $$7.moveTo(pOrigin.x, pOrigin.y, pOrigin.z, $$7.getYRot(), $$7.getXRot());
            }
         }
      }
   }

   @Override
   public MapCodec<SummonEntityEffect> codec() {
      return CODEC;
   }
}
