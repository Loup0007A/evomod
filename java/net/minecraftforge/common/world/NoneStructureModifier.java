package net.minecraftforge.common.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;

public class NoneStructureModifier implements StructureModifier {
   public static final NoneStructureModifier INSTANCE = new NoneStructureModifier();
   public static final MapCodec<NoneStructureModifier> CODEC = MapCodec.unit(INSTANCE);

   @Override
   public void modify(Holder<Structure> structure, StructureModifier.Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
   }

   @Override
   public MapCodec<? extends StructureModifier> codec() {
      return CODEC;
   }
}
