package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PartDefinition {
   private final List<CubeDefinition> cubes;
   private final PartPose partPose;
   private final Map<String, PartDefinition> children = Maps.newHashMap();

   PartDefinition(List<CubeDefinition> pCubes, PartPose pPartPose) {
      this.cubes = pCubes;
      this.partPose = pPartPose;
   }

   public PartDefinition addOrReplaceChild(String pName, CubeListBuilder pCubes, PartPose pPartPose) {
      PartDefinition $$3 = new PartDefinition(pCubes.getCubes(), pPartPose);
      PartDefinition $$4 = this.children.put(pName, $$3);
      if ($$4 != null) {
         $$3.children.putAll($$4.children);
      }

      return $$3;
   }

   public ModelPart bake(int pTexWidth, int pTexHeight) {
      Object2ObjectArrayMap<String, ModelPart> $$2 = this.children
         .entrySet()
         .stream()
         .collect(
            Collectors.toMap(
               Entry::getKey,
               p_171593_ -> ((PartDefinition)p_171593_.getValue()).bake(pTexWidth, pTexHeight),
               (p_171595_, p_171596_) -> p_171595_,
               Object2ObjectArrayMap::new
            )
         );
      List<ModelPart.Cube> $$3 = this.cubes.stream().map(p_171589_ -> p_171589_.bake(pTexWidth, pTexHeight)).collect(ImmutableList.toImmutableList());
      ModelPart $$4 = new ModelPart($$3, $$2);
      $$4.setInitialPose(this.partPose);
      $$4.loadPose(this.partPose);
      return $$4;
   }

   public PartDefinition getChild(String pName) {
      return this.children.get(pName);
   }
}
