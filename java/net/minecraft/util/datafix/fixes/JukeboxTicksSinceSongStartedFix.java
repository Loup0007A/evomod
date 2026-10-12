package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class JukeboxTicksSinceSongStartedFix extends NamedEntityFix {
   public JukeboxTicksSinceSongStartedFix(Schema pOutputSchema) {
      super(pOutputSchema, false, "JukeboxTicksSinceSongStartedFix", References.BLOCK_ENTITY, "minecraft:jukebox");
   }

   public Dynamic<?> fixTag(Dynamic<?> p_344094_) {
      long $$1 = p_344094_.get("TickCount").asLong(0L) - p_344094_.get("RecordStartTick").asLong(0L);
      Dynamic<?> $$2 = p_344094_.remove("IsPlaying").remove("TickCount").remove("RecordStartTick");
      return $$1 > 0L ? $$2.set("ticks_since_song_started", p_344094_.createLong($$1)) : $$2;
   }

   @Override
   protected Typed<?> fix(Typed<?> pTyped) {
      return pTyped.update(DSL.remainderFinder(), this::fixTag);
   }
}
