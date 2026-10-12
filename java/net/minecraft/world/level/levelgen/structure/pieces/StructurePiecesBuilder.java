package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;

public class StructurePiecesBuilder implements StructurePieceAccessor {
   private final List<StructurePiece> pieces = Lists.newArrayList();

   @Override
   public void addPiece(StructurePiece pPiece) {
      this.pieces.add(pPiece);
   }

   @Nullable
   @Override
   public StructurePiece findCollisionPiece(BoundingBox pBox) {
      return StructurePiece.findCollisionPiece(this.pieces, pBox);
   }

   @Deprecated
   public void offsetPiecesVertically(int pOffset) {
      for (StructurePiece $$1 : this.pieces) {
         $$1.move(0, pOffset, 0);
      }
   }

   @Deprecated
   public int moveBelowSeaLevel(int pSeaLevel, int pMinY, RandomSource pRandom, int pAmount) {
      int $$4 = pSeaLevel - pAmount;
      BoundingBox $$5 = this.getBoundingBox();
      int $$6 = $$5.getYSpan() + pMinY + 1;
      if ($$6 < $$4) {
         $$6 += pRandom.nextInt($$4 - $$6);
      }

      int $$7 = $$6 - $$5.maxY();
      this.offsetPiecesVertically($$7);
      return $$7;
   }

   /** @deprecated */
   public void moveInsideHeights(RandomSource pRandom, int pMinY, int pMaxY) {
      BoundingBox $$3 = this.getBoundingBox();
      int $$4 = pMaxY - pMinY + 1 - $$3.getYSpan();
      int $$5;
      if ($$4 > 1) {
         $$5 = pMinY + pRandom.nextInt($$4);
      } else {
         $$5 = pMinY;
      }

      int $$7 = $$5 - $$3.minY();
      this.offsetPiecesVertically($$7);
   }

   public PiecesContainer build() {
      return new PiecesContainer(this.pieces);
   }

   public void clear() {
      this.pieces.clear();
   }

   public boolean isEmpty() {
      return this.pieces.isEmpty();
   }

   public BoundingBox getBoundingBox() {
      return StructurePiece.createBoundingBox(this.pieces.stream());
   }
}
