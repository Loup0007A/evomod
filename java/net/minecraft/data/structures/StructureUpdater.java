package net.minecraft.data.structures;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public class StructureUpdater implements SnbtToNbt.Filter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PREFIX = PackType.SERVER_DATA.getDirectory() + "/minecraft/structure/";

   @Override
   public CompoundTag apply(String pStructureLocationPath, CompoundTag pTag) {
      return pStructureLocationPath.startsWith(PREFIX) ? update(pStructureLocationPath, pTag) : pTag;
   }

   public static CompoundTag update(String pStructureLocationPath, CompoundTag pTag) {
      StructureTemplate $$2 = new StructureTemplate();
      int $$3 = NbtUtils.getDataVersion(pTag, 500);
      int $$4 = 3937;
      if ($$3 < 3937) {
         LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", new Object[]{$$3, 3937, pStructureLocationPath});
      }

      CompoundTag $$5 = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), pTag, $$3);
      $$2.load(BuiltInRegistries.BLOCK.asLookup(), $$5);
      return $$2.save(new CompoundTag());
   }
}
