package net.minecraft.advancements.critereon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public record LightningBoltPredicate(MinMaxBounds.Ints blocksSetOnFire, Optional<EntityPredicate> entityStruck) implements EntitySubPredicate {
   public static final MapCodec<LightningBoltPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_325227_ -> p_325227_.group(
               MinMaxBounds.Ints.CODEC.optionalFieldOf("blocks_set_on_fire", MinMaxBounds.Ints.ANY).forGetter(LightningBoltPredicate::blocksSetOnFire),
               EntityPredicate.CODEC.optionalFieldOf("entity_struck").forGetter(LightningBoltPredicate::entityStruck)
            )
            .apply(p_325227_, LightningBoltPredicate::new)
   );

   public static LightningBoltPredicate blockSetOnFire(MinMaxBounds.Ints pBlocksSetOnFire) {
      return new LightningBoltPredicate(pBlocksSetOnFire, Optional.empty());
   }

   @Override
   public MapCodec<LightningBoltPredicate> codec() {
      return EntitySubPredicates.LIGHTNING;
   }

   @Override
   public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
      return !(pEntity instanceof LightningBolt $$3)
         ? false
         : this.blocksSetOnFire.matches($$3.getBlocksSetOnFire())
            && (this.entityStruck.isEmpty() || $$3.getHitEntities().anyMatch(p_299409_ -> this.entityStruck.get().matches(pLevel, pPosition, p_299409_)));
   }
}
