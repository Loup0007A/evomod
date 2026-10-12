package com.evolutionmod.items;

import com.evolutionmod.brew.BrewRecipe;
import com.evolutionmod.core.EvolutionCapability;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.core.Phenotype;
import com.evolutionmod.genetics.GeneType;
import com.evolutionmod.genetics.Genome;
import com.evolutionmod.network.EvoNetwork;
import com.evolutionmod.network.NotifyMsg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mixture génétique sortie d'un chaudron. La boire fait muter le joueur dans
 * un sens dicté par les ingrédients... et le hasard. Les mixtures toxiques
 * empoisonnent, et les pires peuvent tuer.
 */
public class EvolutionPotionItem extends Item {

    public EvolutionPotionItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static ItemStack create(List<String> ingredients, long seed) {
        ItemStack stack = new ItemStack(ModItems.EVOLUTION_POTION.get());
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String s : ingredients) list.add(StringTag.valueOf(s));
        tag.put("Ingredients", list);
        tag.putLong("Seed", seed);
        tag.putInt("Color", BrewRecipe.compute(ingredients, seed).color());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? new CompoundTag() : d.copyTag();
    }

    private static List<String> ingredients(ItemStack stack) {
        ListTag l = data(stack).getList("Ingredients", 8);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    /** Couleur du liquide (teinte de l'objet). */
    public static int color(ItemStack stack) {
        CompoundTag t = data(stack);
        return t.contains("Color") ? t.getInt("Color") : 0x8E5AFF;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide()) {
            BrewRecipe.Outcome out = BrewRecipe.compute(ingredients(stack), data(stack).getLong("Seed"));
            apply(user, out);
        }
        if (user instanceof Player p && p.getAbilities().instabuild) return stack;
        stack.shrink(1);
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (user instanceof Player p && !p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return stack;
    }

    /** Applique la mixture : mutations du génome puis effets toxiques. Public pour les tests. */
    public static List<String> apply(LivingEntity user, BrewRecipe.Outcome out) {
        List<String> summary = new ArrayList<>();
        Genome g = EvolutionCapability.genome(user);
        List<Map.Entry<GeneType, Double>> sorted = new ArrayList<>(out.deltas().entrySet());
        sorted.sort(Comparator.comparingDouble((Map.Entry<GeneType, Double> e) -> -Math.abs(e.getValue())));

        if (g != null) {
            for (Map.Entry<GeneType, Double> e : sorted) {
                GeneType t = e.getKey();
                if (!g.has(t) || Math.abs(e.getValue()) < 0.05) continue;
                g.set(t, g.get(t) + e.getValue() * t.range() * 0.05);
                summary.add((e.getValue() > 0 ? "+" : "-") + t.name());
            }
            Phenotype.apply(user, g, false);
            EvolutionManager.sync(user);
        } else {
            // Évolution du joueur désactivée : simples effets passagers
            for (Map.Entry<GeneType, Double> e : sorted) {
                if (e.getValue() <= 0.3) continue;
                switch (e.getKey()) {
                    case SPEED -> user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0));
                    case STRENGTH -> user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
                    case REGENERATION -> user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0));
                    case FIRE_RESISTANCE -> user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 0));
                    case AGILITY -> user.addEffect(new MobEffectInstance(MobEffects.JUMP, 1200, 0));
                    case ARMOR, VITALITY -> user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0));
                    default -> { }
                }
            }
        }

        double tox = out.toxicity();
        if (tox >= 1) {
            user.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            user.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (100 + tox * 40), 0));
        }
        if (tox >= 2.5) {
            user.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (200 + tox * 40), 1));
            user.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
            user.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 1));
        }
        if (tox >= 4) {
            user.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
            // Les pires mixtures peuvent tuer
            user.hurt(user.damageSources().magic(), (float) ((tox - 3) * 4));
        }

        user.level().playSound(null, user.blockPosition(), tox >= 2.5 ? SoundEvents.WITCH_DRINK : SoundEvents.GENERIC_DRINK,
            SoundSource.PLAYERS, 1f, tox >= 2.5 ? 0.6f : 1.1f);
        if (user instanceof ServerPlayer sp) {
            List<String> args = new ArrayList<>();
            for (int i = 0; i < Math.min(3, summary.size()); i++) {
                String s = summary.get(i);
                args.add(s.charAt(0) + " ");
                args.add("#gene:" + s.substring(1));
            }
            String key;
            if (summary.isEmpty()) key = tox >= 1 ? "evolutionmod.potion.toxic_only" : "evolutionmod.potion.nothing";
            else key = "evolutionmod.potion.result" + Math.min(3, summary.size());
            EvoNetwork.sendToPlayer(sp, new NotifyMsg(tox >= 2.5 ? NotifyMsg.Kind.DANGER : NotifyMsg.Kind.BREW, key, args));
        }
        return summary;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        List<String> ing = ingredients(stack);
        if (ing.isEmpty()) return;
        tooltip.add(Component.translatable("evolutionmod.potion.ingredients", ing.size()).withStyle(ChatFormatting.GRAY));
        for (String id : ing) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(id));
            if (item != null) {
                tooltip.add(Component.literal(" • ").append(item.getDescription()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.add(Component.translatable("evolutionmod.potion.warning").withStyle(ChatFormatting.DARK_RED,
            ChatFormatting.ITALIC));
    }
}
