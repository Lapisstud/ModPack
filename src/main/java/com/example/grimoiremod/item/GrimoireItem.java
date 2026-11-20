package com.example.grimoiremod.item;

import com.example.grimoiremod.data.GlyphPage;
import com.example.grimoiremod.data.GrimoireData;
import com.example.grimoiremod.data.SkillTreeData;
import com.example.grimoiremod.data.SpellParser;
import com.example.grimoiremod.entity.SpellProjectile;
import com.example.grimoiremod.glyphs.GrimoireEffects;
import com.example.grimoiremod.glyphs.SpellAggregator;
import com.example.grimoiremod.glyphs.Stat;
import com.example.grimoiremod.glyphs.StatBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class GrimoireItem extends Item {
    private static final float BASE_SPREAD_PER_SPLIT = 0.15f;

    public GrimoireItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSpectator()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            float cd = cast(level, player, stack);
            if (cd > 0f) player.getCooldowns().addCooldown(this, Math.max(1, (int)(cd * 20)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private float cast(Level level, Player player, ItemStack stack) {
        int active = GrimoireData.getActivePage(stack);
        GlyphPage page = GrimoireData.getPage(stack, active);

        SpellParser.Parsed parsed = SpellParser.parse(page);
        if (parsed.formId() == null) return 0f;

        StatBlock sb = SpellAggregator.aggregate(page);
        List<SpellParser.Element> elements = parsed.elements();

        switch (parsed.formId()) {
            case "projectile", "fireball" -> castProjectile(level, player, sb, elements);
            case "self"       -> castSelf(player, sb, elements);
            case "touch"      -> castTouch(level, player, sb, elements);
        }
        
        // Перезарядка теперь полностью высчитывается внутри SpellAggregator
        return sb.getFloat(Stat.COOLDOWN);
    }

    private void castProjectile(Level level, Player player, StatBlock sb, List<SpellParser.Element> elements) {
        int count = sb.getInt(Stat.COUNT);
        float angle = sb.getFloat(Stat.ANGLE);
        float spreadPerSplit = BASE_SPREAD_PER_SPLIT * angle;
        float critFactor = rollCrit(player, sb);
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < count; i++) {
            double yawOffset = (i - (count - 1) / 2.0) * spreadPerSplit;
            Vec3 dir = look.yRot((float) yawOffset);
            level.addFreshEntity(new SpellProjectile(level, player, elements, sb, critFactor, dir));
        }
    }

    private void castSelf(Player player, StatBlock sb, List<SpellParser.Element> elements) {
        float critFactor = rollCrit(player, sb);
        for (SpellParser.Element e : elements) GrimoireEffects.apply(e, sb, player, critFactor, player, null);
    }

    private void castTouch(Level level, Player player, StatBlock sb, List<SpellParser.Element> elements) {
        AABB box = player.getBoundingBox().inflate(3);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player);
        if (targets.isEmpty()) return;
        LivingEntity closest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity t : targets) {
            double d = t.distanceToSqr(player);
            if (d < best) { best = d; closest = t; }
        }
        if (closest == null) return;
        float critFactor = rollCrit(player, sb);
        for (SpellParser.Element e : elements) GrimoireEffects.apply(e, sb, closest, critFactor, player, null);
    }

    private static float rollCrit(Player player, StatBlock sb) {
        boolean crit = player.getRandom().nextFloat() < sb.getFloat(Stat.CRIT_CHANCE);
        return crit ? (1f + sb.getFloat(Stat.CRIT_MULT)) : 1f;
    }
}
