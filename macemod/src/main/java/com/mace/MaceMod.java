package com.mace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.ConstantLootTableRange;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.SetNbtLootFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.List;

public class MaceMod implements ModInitializer {
    public static final String ID = "mace";

    public static final ItemGroup GROUP = FabricItemGroupBuilder.create(new Identifier(ID, "general"))
            .icon(() -> new ItemStack(MaceMod.MACE))
            .appendItems(list -> {
                list.add(new ItemStack(MaceMod.MACE));
                list.add(new ItemStack(MaceMod.WIND_CHARGE));
                for (int i = 1; i <= 5; i++) list.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(MaceMod.DENSITY, i)));
                for (int i = 1; i <= 4; i++) list.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(MaceMod.BREACH, i)));
                for (int i = 1; i <= 3; i++) list.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(MaceMod.WIND_BURST, i)));
            }).build();

    public static final Item MACE = new MaceItem(new Item.Settings().group(GROUP).maxDamage(500));
    public static final Item WIND_CHARGE = new WindChargeItem(new Item.Settings().group(GROUP).maxCount(64));

    public static final Enchantment DENSITY = new MaceEnchantment(Enchantment.Rarity.UNCOMMON, 5, MaceEnchantment.Kind.DENSITY);
    public static final Enchantment BREACH = new MaceEnchantment(Enchantment.Rarity.RARE, 4, MaceEnchantment.Kind.BREACH);
    public static final Enchantment WIND_BURST = new MaceEnchantment(Enchantment.Rarity.RARE, 3, MaceEnchantment.Kind.WIND);

    public static final EntityType<WindChargeEntity> WIND_CHARGE_ENTITY = FabricEntityTypeBuilder
            .<WindChargeEntity>create(SpawnGroup.MISC, WindChargeEntity::new)
            .dimensions(EntityDimensions.fixed(0.3125f, 0.3125f))
            .trackRangeBlocks(64).trackedUpdateRate(10).build();

    private static final List<Identifier> CHESTS = Arrays.asList(
            new Identifier("minecraft", "chests/simple_dungeon"),
            new Identifier("minecraft", "chests/end_city_treasure"),
            new Identifier("minecraft", "chests/bastion_treasure"),
            new Identifier("minecraft", "chests/stronghold_library"),
            new Identifier("minecraft", "chests/abandoned_mineshaft"));

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(ID, "mace"), MACE);
        Registry.register(Registry.ITEM, new Identifier(ID, "wind_charge"), WIND_CHARGE);
        Registry.register(Registry.ENCHANTMENT, new Identifier(ID, "density"), DENSITY);
        Registry.register(Registry.ENCHANTMENT, new Identifier(ID, "breach"), BREACH);
        Registry.register(Registry.ENCHANTMENT, new Identifier(ID, "wind_burst"), WIND_BURST);
        Registry.register(Registry.ENTITY_TYPE, new Identifier(ID, "wind_charge"), WIND_CHARGE_ENTITY);

        LootTableLoadingCallback.EVENT.register((rm, lootManager, id, supplier, setter) -> {
            if (!CHESTS.contains(id)) return;
            supplier.withPool(pool(ItemEntry.builder(MACE).build(), 0.12f));
            supplier.withPool(pool(book("mace:density", 3), 0.12f));
            supplier.withPool(pool(book("mace:breach", 2), 0.10f));
            supplier.withPool(pool(book("mace:wind_burst", 2), 0.10f));
            supplier.withPool(pool(ItemEntry.builder(WIND_CHARGE).build(), 0.30f));
        });
    }

    private static net.minecraft.loot.LootPool pool(LootPoolEntry entry, float chance) {
        return FabricLootPoolBuilder.builder().rolls(ConstantLootTableRange.create(1))
                .withCondition(RandomChanceLootCondition.builder(chance).build())
                .withEntry(entry).build();
    }

    private static LootPoolEntry book(String ench, int lvl) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag e = new CompoundTag();
        e.putString("id", ench);
        e.putShort("lvl", (short) lvl);
        list.add(e);
        tag.put("StoredEnchantments", list);
        return ItemEntry.builder(Items.ENCHANTED_BOOK).apply(SetNbtLootFunction.builder(tag)).build();
    }
}
