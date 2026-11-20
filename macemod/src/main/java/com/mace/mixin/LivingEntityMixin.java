package com.mace.mixin;

import com.mace.MaceItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float mace$smash(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.world.isClient) return amount;
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity)) return amount;
        if (source.getSource() != attacker || source.isProjectile() || source.isExplosive() || !"player".equals(source.getName())) return amount;
        ServerPlayerEntity p = (ServerPlayerEntity) attacker;
        ItemStack stack = p.getMainHandStack();
        if (!(stack.getItem() instanceof MaceItem) || !MaceItem.canSmash(p)) return amount;
        return MaceItem.smash(p, self, stack, amount);
    }
}
