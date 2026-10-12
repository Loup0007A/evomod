package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.VillagerHeadModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerProfessionLayer<T extends LivingEntity & VillagerDataHolder, M extends EntityModel<T> & VillagerHeadModel> extends RenderLayer<T, M> {
   private static final Int2ObjectMap<ResourceLocation> LEVEL_LOCATIONS = Util.make(new Int2ObjectOpenHashMap(), p_340946_ -> {
      p_340946_.put(1, ResourceLocation.withDefaultNamespace("stone"));
      p_340946_.put(2, ResourceLocation.withDefaultNamespace("iron"));
      p_340946_.put(3, ResourceLocation.withDefaultNamespace("gold"));
      p_340946_.put(4, ResourceLocation.withDefaultNamespace("emerald"));
      p_340946_.put(5, ResourceLocation.withDefaultNamespace("diamond"));
   });
   private final Object2ObjectMap<VillagerType, VillagerMetaDataSection.Hat> typeHatCache = new Object2ObjectOpenHashMap();
   private final Object2ObjectMap<VillagerProfession, VillagerMetaDataSection.Hat> professionHatCache = new Object2ObjectOpenHashMap();
   private final ResourceManager resourceManager;
   private final String path;

   public VillagerProfessionLayer(RenderLayerParent<T, M> pRenderer, ResourceManager pResourceManager, String pPath) {
      super(pRenderer);
      this.resourceManager = pResourceManager;
      this.path = pPath;
   }

   public void render(
      PoseStack pPoseStack,
      MultiBufferSource pBuffer,
      int pPackedLight,
      T pLivingEntity,
      float pLimbSwing,
      float pLimbSwingAmount,
      float pPartialTicks,
      float pAgeInTicks,
      float pNetHeadYaw,
      float pHeadPitch
   ) {
      if (!pLivingEntity.isInvisible()) {
         VillagerData $$10 = pLivingEntity.getVillagerData();
         VillagerType $$11 = $$10.getType();
         VillagerProfession $$12 = $$10.getProfession();
         VillagerMetaDataSection.Hat $$13 = this.getHatData(this.typeHatCache, "type", BuiltInRegistries.VILLAGER_TYPE, $$11);
         VillagerMetaDataSection.Hat $$14 = this.getHatData(this.professionHatCache, "profession", BuiltInRegistries.VILLAGER_PROFESSION, $$12);
         M $$15 = this.getParentModel();
         $$15.hatVisible($$14 == VillagerMetaDataSection.Hat.NONE || $$14 == VillagerMetaDataSection.Hat.PARTIAL && $$13 != VillagerMetaDataSection.Hat.FULL);
         ResourceLocation $$16 = this.getResourceLocation("type", BuiltInRegistries.VILLAGER_TYPE.getKey($$11));
         renderColoredCutoutModel($$15, $$16, pPoseStack, pBuffer, pPackedLight, pLivingEntity, -1);
         $$15.hatVisible(true);
         if ($$12 != VillagerProfession.NONE && !pLivingEntity.isBaby()) {
            ResourceLocation $$17 = this.getResourceLocation("profession", BuiltInRegistries.VILLAGER_PROFESSION.getKey($$12));
            renderColoredCutoutModel($$15, $$17, pPoseStack, pBuffer, pPackedLight, pLivingEntity, -1);
            if ($$12 != VillagerProfession.NITWIT) {
               ResourceLocation $$18 = this.getResourceLocation(
                  "profession_level", (ResourceLocation)LEVEL_LOCATIONS.get(Mth.clamp($$10.getLevel(), 1, LEVEL_LOCATIONS.size()))
               );
               renderColoredCutoutModel($$15, $$18, pPoseStack, pBuffer, pPackedLight, pLivingEntity, -1);
            }
         }
      }
   }

   private ResourceLocation getResourceLocation(String pFolder, ResourceLocation pLocation) {
      return pLocation.withPath(p_247944_ -> "textures/entity/" + this.path + "/" + pFolder + "/" + p_247944_ + ".png");
   }

   public <K> VillagerMetaDataSection.Hat getHatData(
      Object2ObjectMap<K, VillagerMetaDataSection.Hat> pCache, String pFolder, DefaultedRegistry<K> pVillagerTypeRegistry, K pKey
   ) {
      return (VillagerMetaDataSection.Hat)pCache.computeIfAbsent(
         pKey, p_258159_ -> this.resourceManager.getResource(this.getResourceLocation(pFolder, pVillagerTypeRegistry.getKey(pKey))).flatMap(p_234875_ -> {
               try {
                  return p_234875_.metadata().getSection(VillagerMetaDataSection.SERIALIZER).map(VillagerMetaDataSection::getHat);
               } catch (IOException var2x) {
                  return Optional.empty();
               }
            }).orElse(VillagerMetaDataSection.Hat.NONE)
      );
   }
}
