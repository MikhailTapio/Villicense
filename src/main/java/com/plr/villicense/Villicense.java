package com.plr.villicense;

import com.plr.villicense.item.VillagerLicenseItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class Villicense implements ModInitializer {
    public static final Item VILLAGER_LICENSE = new VillagerLicenseItem();

    @Override
    public void onInitialize() {
        Registry.register(
                BuiltInRegistries.ITEM,
                new ResourceLocation("villicense", "villager_license"),
                VILLAGER_LICENSE
        );
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(c -> c.accept(VILLAGER_LICENSE));
    }
}
