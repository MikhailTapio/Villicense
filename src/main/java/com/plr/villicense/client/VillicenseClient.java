package com.plr.villicense.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class VillicenseClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
    }

    public static Component getName4CrouchingKey() {
        return Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage()
                .copy().withStyle(style -> style.withColor(ChatFormatting.YELLOW));
    }
}
