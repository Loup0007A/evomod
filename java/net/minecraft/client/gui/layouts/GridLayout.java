package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GridLayout extends AbstractLayout {
   private final List<LayoutElement> children = new ArrayList<>();
   private final List<GridLayout.CellInhabitant> cellInhabitants = new ArrayList<>();
   private final LayoutSettings defaultCellSettings = LayoutSettings.defaults();
   private int rowSpacing = 0;
   private int columnSpacing = 0;

   public GridLayout() {
      this(0, 0);
   }

   public GridLayout(int pX, int pY) {
      super(pX, pY, 0, 0);
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      int $$0 = 0;
      int $$1 = 0;

      for (GridLayout.CellInhabitant $$2 : this.cellInhabitants) {
         $$0 = Math.max($$2.getLastOccupiedRow(), $$0);
         $$1 = Math.max($$2.getLastOccupiedColumn(), $$1);
      }

      int[] $$3 = new int[$$1 + 1];
      int[] $$4 = new int[$$0 + 1];

      for (GridLayout.CellInhabitant $$5 : this.cellInhabitants) {
         int $$6 = $$5.getHeight() - ($$5.occupiedRows - 1) * this.rowSpacing;
         Divisor $$7 = new Divisor($$6, $$5.occupiedRows);

         for (int $$8 = $$5.row; $$8 <= $$5.getLastOccupiedRow(); $$8++) {
            $$4[$$8] = Math.max($$4[$$8], $$7.nextInt());
         }

         int $$9 = $$5.getWidth() - ($$5.occupiedColumns - 1) * this.columnSpacing;
         Divisor $$10 = new Divisor($$9, $$5.occupiedColumns);

         for (int $$11 = $$5.column; $$11 <= $$5.getLastOccupiedColumn(); $$11++) {
            $$3[$$11] = Math.max($$3[$$11], $$10.nextInt());
         }
      }

      int[] $$12 = new int[$$1 + 1];
      int[] $$13 = new int[$$0 + 1];
      $$12[0] = 0;

      for (int $$14 = 1; $$14 <= $$1; $$14++) {
         $$12[$$14] = $$12[$$14 - 1] + $$3[$$14 - 1] + this.columnSpacing;
      }

      $$13[0] = 0;

      for (int $$15 = 1; $$15 <= $$0; $$15++) {
         $$13[$$15] = $$13[$$15 - 1] + $$4[$$15 - 1] + this.rowSpacing;
      }

      for (GridLayout.CellInhabitant $$16 : this.cellInhabitants) {
         int $$17 = 0;

         for (int $$18 = $$16.column; $$18 <= $$16.getLastOccupiedColumn(); $$18++) {
            $$17 += $$3[$$18];
         }

         $$17 += this.columnSpacing * ($$16.occupiedColumns - 1);
         $$16.setX(this.getX() + $$12[$$16.column], $$17);
         int $$19 = 0;

         for (int $$20 = $$16.row; $$20 <= $$16.getLastOccupiedRow(); $$20++) {
            $$19 += $$4[$$20];
         }

         $$19 += this.rowSpacing * ($$16.occupiedRows - 1);
         $$16.setY(this.getY() + $$13[$$16.row], $$19);
      }

      this.width = $$12[$$1] + $$3[$$1];
      this.height = $$13[$$0] + $$4[$$0];
   }

   public <T extends LayoutElement> T addChild(T pChild, int pRow, int pColumn) {
      return this.addChild(pChild, pRow, pColumn, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T pChild, int pRow, int pColumn, LayoutSettings pLayoutSettings) {
      return this.addChild(pChild, pRow, pColumn, 1, 1, pLayoutSettings);
   }

   public <T extends LayoutElement> T addChild(T pChild, int pRow, int pColumn, Consumer<LayoutSettings> pLayoutSettingsFactory) {
      return this.addChild(pChild, pRow, pColumn, 1, 1, Util.make(this.newCellSettings(), pLayoutSettingsFactory));
   }

   public <T extends LayoutElement> T addChild(T pChild, int pRow, int pColumn, int pOccupiedRows, int pOccupiedColumns) {
      return this.addChild(pChild, pRow, pColumn, pOccupiedRows, pOccupiedColumns, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T pChild, int pRow, int pColumn, int pOccupiedRows, int pOccupiedColumns, LayoutSettings pLayoutSettings) {
      if (pOccupiedRows < 1) {
         throw new IllegalArgumentException("Occupied rows must be at least 1");
      } else if (pOccupiedColumns < 1) {
         throw new IllegalArgumentException("Occupied columns must be at least 1");
      } else {
         this.cellInhabitants.add(new GridLayout.CellInhabitant(pChild, pRow, pColumn, pOccupiedRows, pOccupiedColumns, pLayoutSettings));
         this.children.add(pChild);
         return pChild;
      }
   }

   public <T extends LayoutElement> T addChild(
      T pChild, int pRow, int pColumn, int pOccupiedRows, int pOccupiedColumns, Consumer<LayoutSettings> pLayoutSettingsFactory
   ) {
      return this.addChild(pChild, pRow, pColumn, pOccupiedRows, pOccupiedColumns, Util.make(this.newCellSettings(), pLayoutSettingsFactory));
   }

   public GridLayout columnSpacing(int pColumnSpacing) {
      this.columnSpacing = pColumnSpacing;
      return this;
   }

   public GridLayout rowSpacing(int pRowSpacing) {
      this.rowSpacing = pRowSpacing;
      return this;
   }

   public GridLayout spacing(int pSpacing) {
      return this.columnSpacing(pSpacing).rowSpacing(pSpacing);
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> pVisitor) {
      this.children.forEach(pVisitor);
   }

   public LayoutSettings newCellSettings() {
      return this.defaultCellSettings.copy();
   }

   public LayoutSettings defaultCellSetting() {
      return this.defaultCellSettings;
   }

   public GridLayout.RowHelper createRowHelper(int pColumns) {
      return new GridLayout.RowHelper(pColumns);
   }

   @OnlyIn(Dist.CLIENT)
   static class CellInhabitant extends AbstractLayout.AbstractChildWrapper {
      final int row;
      final int column;
      final int occupiedRows;
      final int occupiedColumns;

      CellInhabitant(LayoutElement pChild, int pRow, int pColumn, int pOccupiedRows, int pOccupiedColumns, LayoutSettings pLayoutSettings) {
         super(pChild, pLayoutSettings.getExposed());
         this.row = pRow;
         this.column = pColumn;
         this.occupiedRows = pOccupiedRows;
         this.occupiedColumns = pOccupiedColumns;
      }

      public int getLastOccupiedRow() {
         return this.row + this.occupiedRows - 1;
      }

      public int getLastOccupiedColumn() {
         return this.column + this.occupiedColumns - 1;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public final class RowHelper {
      private final int columns;
      private int index;

      RowHelper(final int pColumns) {
         this.columns = pColumns;
      }

      public <T extends LayoutElement> T addChild(T pChild) {
         return this.addChild(pChild, 1);
      }

      public <T extends LayoutElement> T addChild(T pChild, int pOccupiedColumns) {
         return this.addChild(pChild, pOccupiedColumns, this.defaultCellSetting());
      }

      public <T extends LayoutElement> T addChild(T pChild, LayoutSettings pLayoutSettings) {
         return this.addChild(pChild, 1, pLayoutSettings);
      }

      public <T extends LayoutElement> T addChild(T pChild, int pOccupiedColumns, LayoutSettings pLayoutSettings) {
         int $$3 = this.index / this.columns;
         int $$4 = this.index % this.columns;
         if ($$4 + pOccupiedColumns > this.columns) {
            $$3++;
            $$4 = 0;
            this.index = Mth.roundToward(this.index, this.columns);
         }

         this.index += pOccupiedColumns;
         return GridLayout.this.addChild(pChild, $$3, $$4, 1, pOccupiedColumns, pLayoutSettings);
      }

      public GridLayout getGrid() {
         return GridLayout.this;
      }

      public LayoutSettings newCellSettings() {
         return GridLayout.this.newCellSettings();
      }

      public LayoutSettings defaultCellSetting() {
         return GridLayout.this.defaultCellSetting();
      }
   }
}
