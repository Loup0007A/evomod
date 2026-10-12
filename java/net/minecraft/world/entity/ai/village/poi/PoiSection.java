package net.minecraft.world.entity.ai.village.poi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.VisibleForDebug;
import org.slf4j.Logger;

public class PoiSection {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Short2ObjectMap<PoiRecord> records = new Short2ObjectOpenHashMap();
   private final Map<Holder<PoiType>, Set<PoiRecord>> byType = Maps.newHashMap();
   private final Runnable setDirty;
   private boolean isValid;

   public static Codec<PoiSection> codec(Runnable pExecutable) {
      return RecordCodecBuilder.create(
            p_326968_ -> p_326968_.group(
                     RecordCodecBuilder.point(pExecutable),
                     Codec.BOOL.lenientOptionalFieldOf("Valid", false).forGetter(p_148681_ -> p_148681_.isValid),
                     PoiRecord.codec(pExecutable).listOf().fieldOf("Records").forGetter(p_148675_ -> ImmutableList.copyOf(p_148675_.records.values()))
                  )
                  .apply(p_326968_, PoiSection::new)
         )
         .orElseGet(Util.prefix("Failed to read POI section: ", LOGGER::error), () -> new PoiSection(pExecutable, false, ImmutableList.of()));
   }

   public PoiSection(Runnable pSetDirty) {
      this(pSetDirty, true, ImmutableList.of());
   }

   private PoiSection(Runnable p_27269_, boolean p_27270_, List<PoiRecord> p_27271_) {
      this.setDirty = p_27269_;
      this.isValid = p_27270_;
      p_27271_.forEach(this::add);
   }

   public Stream<PoiRecord> getRecords(Predicate<Holder<PoiType>> pTypePredicate, PoiManager.Occupancy pStatus) {
      return this.byType
         .entrySet()
         .stream()
         .filter(p_27309_ -> pTypePredicate.test(p_27309_.getKey()))
         .flatMap(p_27301_ -> p_27301_.getValue().stream())
         .filter(pStatus.getTest());
   }

   public void add(BlockPos pPos, Holder<PoiType> pType) {
      if (this.add(new PoiRecord(pPos, pType, this.setDirty))) {
         LOGGER.debug("Added POI of type {} @ {}", pType.getRegisteredName(), pPos);
         this.setDirty.run();
      }
   }

   private boolean add(PoiRecord p_27274_) {
      BlockPos $$1 = p_27274_.getPos();
      Holder<PoiType> $$2 = p_27274_.getPoiType();
      short $$3 = SectionPos.sectionRelativePos($$1);
      PoiRecord $$4 = (PoiRecord)this.records.get($$3);
      if ($$4 != null) {
         if ($$2.equals($$4.getPoiType())) {
            return false;
         }

         Util.logAndPauseIfInIde("POI data mismatch: already registered at " + $$1);
      }

      this.records.put($$3, p_27274_);
      this.byType.computeIfAbsent($$2, p_218029_ -> Sets.newHashSet()).add(p_27274_);
      return true;
   }

   public void remove(BlockPos pPos) {
      PoiRecord $$1 = (PoiRecord)this.records.remove(SectionPos.sectionRelativePos(pPos));
      if ($$1 == null) {
         LOGGER.error("POI data mismatch: never registered at {}", pPos);
      } else {
         this.byType.get($$1.getPoiType()).remove($$1);
         LOGGER.debug("Removed POI of type {} @ {}", LogUtils.defer($$1::getPoiType), LogUtils.defer($$1::getPos));
         this.setDirty.run();
      }
   }

   @Deprecated
   @VisibleForDebug
   public int getFreeTickets(BlockPos pPos) {
      return this.getPoiRecord(pPos).map(PoiRecord::getFreeTickets).orElse(0);
   }

   public boolean release(BlockPos pPos) {
      PoiRecord $$1 = (PoiRecord)this.records.get(SectionPos.sectionRelativePos(pPos));
      if ($$1 == null) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("POI never registered at " + pPos));
      } else {
         boolean $$2 = $$1.releaseTicket();
         this.setDirty.run();
         return $$2;
      }
   }

   public boolean exists(BlockPos pPos, Predicate<Holder<PoiType>> pTypePredicate) {
      return this.getType(pPos).filter(pTypePredicate).isPresent();
   }

   public Optional<Holder<PoiType>> getType(BlockPos pPos) {
      return this.getPoiRecord(pPos).map(PoiRecord::getPoiType);
   }

   private Optional<PoiRecord> getPoiRecord(BlockPos pPos) {
      return Optional.ofNullable((PoiRecord)this.records.get(SectionPos.sectionRelativePos(pPos)));
   }

   public void refresh(Consumer<BiConsumer<BlockPos, Holder<PoiType>>> pPosToTypeConsumer) {
      if (!this.isValid) {
         Short2ObjectMap<PoiRecord> $$1 = new Short2ObjectOpenHashMap(this.records);
         this.clear();
         pPosToTypeConsumer.accept((p_218032_, p_218033_) -> {
            short $$3 = SectionPos.sectionRelativePos(p_218032_);
            PoiRecord $$4 = (PoiRecord)$$1.computeIfAbsent($$3, p_218027_ -> new PoiRecord(p_218032_, p_218033_, this.setDirty));
            this.add($$4);
         });
         this.isValid = true;
         this.setDirty.run();
      }
   }

   private void clear() {
      this.records.clear();
      this.byType.clear();
   }

   boolean isValid() {
      return this.isValid;
   }
}
