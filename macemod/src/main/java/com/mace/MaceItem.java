package com.mace;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class MaceItem extends Item {
    private final Multimap<EntityAttribute, EntityAttributeModifier> attrs;

    public MaceItem(Settings settings) {
        super(settings);
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> b = ImmutableMultimap.builder();
        b.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", 5.0, EntityAttributeModifier.Operation.ADDITION));
        b.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", -3.4, EntityAttributeModifier.Operation.ADDITION));
        attrs = b.build();
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attrs : super.getAttributeModifiers(slot);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.getHardness(world, pos) != 0.0F) stack.damage(2, miner, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return !miner.isCreative();
    }

    @Override public int getEnchantability() { return 15; }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.getItem() == Items.BLAZE_ROD;
    }

    // ---------- Smash attack logic (called from LivingEntityMixin) ----------

    public static boolean canSmash(PlayerEntity p) {
        return p.fallDistance > 1.5F && !p.isFallFlying() && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater();
    }

    public static float smash(ServerPlayerEntity p, LivingEntity target, ItemStack stack, float amount) {
        float fall = p.fallDistance;
        float bonus = Math.min(fall, 3f) * 4f
                + Math.max(Math.min(fall - 3f, 5f), 0f) * 2f
                + Math.max(fall - 8f, 0f);
        int density = EnchantmentHelper.getLevel(MaceMod.DENSITY, stack);
        bonus += density * 0.5f * fall;
        float total = amount + bonus;

        int breach = EnchantmentHelper.getLevel(MaceMod.BREACH, stack);
        if (breach > 0) total = breach(target, total, breach);

        ServerWorld w = (ServerWorld) p.world;

        // area knockback around the target
        Box box = target.getBoundingBox().expand(3.5);
        List<LivingEntity> near = w.getEntitiesByClass(LivingEntity.class, box, e -> e != p && e != target && e.isAlive());
        for (LivingEntity e : near) {
            double dist = e.distanceTo(target);
            if (dist > 3.5) continue;
            float k = (float) ((3.5 - dist) / 3.5) * 1.3f;
            e.takeKnockback(k, target.getX() - e.getX(), target.getZ() - e.getZ());
            e.addVelocity(0, 0.25 * k, 0);
            if (e instanceof ServerPlayerEntity) ((ServerPlayerEntity) e).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(e));
        }

        w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.7f);
        w.spawnParticles(ParticleTypes.CLOUD, target.getX(), target.getBodyY(0.5), target.getZ(), 15, 0.4, 0.3, 0.4, 0.15);
        w.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0, 0, 0);

        p.fallDistance = 0.0F; // no fall damage after a smash

        int wb = EnchantmentHelper.getLevel(MaceMod.WIND_BURST, stack);
        if (wb > 0) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.75 + 0.35 * wb, v.z);
            p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
            w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 1.0f, 1.6f);
            w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 20, 0.4, 0.1, 0.4, 0.1);
        }
        return total;
    }

    // Breach: each level cuts the target's armor by 15%
    private static float breach(LivingEntity target, float total, int level) {
        float armor = (float) target.getArmor();
        if (armor <= 0) return total;
        float tough = (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        float reduced = armor * Math.max(0f, 1f - 0.15f * level);
        float want = DamageUtil.getDamageLeft(total, reduced, tough);
        float lo = total, hi = total * 6f;
        for (int i = 0; i < 24; i++) {
            float mid = (lo + hi) / 2f;
            if (DamageUtil.getDamageLeft(mid, armor, tough) < want) lo = mid; else hi = mid;
        }
        return hi;
    }
}
