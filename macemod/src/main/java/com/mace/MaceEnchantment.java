package com.mace;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class MaceEnchantment extends Enchantment {
    public enum Kind { DENSITY, BREACH, WIND }

    private final int max;
    private final Kind kind;

    public MaceEnchantment(Rarity rarity, int max, Kind kind) {
        super(rarity, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.max = max;
        this.kind = kind;
    }

    @Override public int getMaxLevel() { return max; }
    @Override public int getMinPower(int level) { return 5 + (level - 1) * 8; }
    @Override public int getMaxPower(int level) { return getMinPower(level) + 20; }
    @Override public boolean isAcceptableItem(ItemStack stack) { return stack.getItem() instanceof MaceItem; }

    @Override
    protected boolean canAccept(Enchantment other) {
        if (other instanceof MaceEnchantment) {
            Kind o = ((MaceEnchantment) other).kind;
            if (o != kind && o != Kind.WIND && kind != Kind.WIND) return false; // density xor breach
        }
        return super.canAccept(other);
    }
}
