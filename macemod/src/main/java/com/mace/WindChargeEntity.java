package com.mace;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class WindChargeEntity extends ThrownItemEntity {
    public WindChargeEntity(EntityType<? extends WindChargeEntity> type, World world) { super(type, world); }

    public WindChargeEntity(World world, LivingEntity owner) { super(MaceMod.WIND_CHARGE_ENTITY, owner, world); }

    @Override protected Item getDefaultItem() { return MaceMod.WIND_CHARGE; }

    @Override protected float getGravity() { return 0.0F; }

    @Override
    public void tick() {
        super.tick();
        if (!world.isClient && age > 100) remove();
        if (world.isClient) world.addParticle(ParticleTypes.CLOUD, getX(), getY(), getZ(), 0, 0, 0);
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        hit.getEntity().damage(DamageSource.thrownProjectile(this, getOwner()), 1.0F);
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (world.isClient) return;
        burst(hit.getPos());
        remove();
    }

    private void burst(Vec3d c) {
        ServerWorld w = (ServerWorld) world;
        Box box = new Box(c.x - 3, c.y - 3, c.z - 3, c.x + 3, c.y + 3, c.z + 3);
        List<Entity> list = w.getEntitiesByClass(Entity.class, box, e -> e != this && !e.isSpectator());
        for (Entity e : list) {
            Vec3d mid = e.getPos().add(0, e.getHeight() * 0.5, 0);
            Vec3d dir = mid.subtract(c);
            double dist = dir.length();
            if (dist > 3.0) continue;
            dir = dist < 0.01 ? new Vec3d(0, 1, 0) : dir.normalize();
            double f = (1.0 - dist / 3.0) * 1.7;
            e.addVelocity(dir.x * f, dir.y * f, dir.z * f);
            e.fallDistance = 0.0F;
            if (e instanceof ServerPlayerEntity) ((ServerPlayerEntity) e).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(e));
        }
        w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 0.5f, 1.9f);
        w.spawnParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 25, 0.5, 0.5, 0.5, 0.15);
        w.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
    }
}
