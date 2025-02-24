package com.plr.villicense.item;

import com.google.common.collect.Lists;
import com.plr.villicense.client.VillicenseClient;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@MethodsReturnNonnullByDefault
public class VillagerLicenseItem extends Item {
    public VillagerLicenseItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack itemStack, Player player, LivingEntity livingEntity, InteractionHand interactionHand) {
        if (player.level().isClientSide() || !interactionHand.equals(InteractionHand.MAIN_HAND) || !(livingEntity instanceof Villager villager)) return InteractionResult.PASS;
        // DO NOT use itemStack in the method's args
        final ItemStack stack = player.getMainHandItem();
        var tag = stack.getOrCreateTag();
        tag.remove("confirm");
        if (!player.isShiftKeyDown()) {
            for (var c : genVillagerInfo(villager)) {
                player.sendSystemMessage(c);
            }
        } else {
            if (tag.contains("pos")) {
                final var posRaw = tag.getLong("pos");
                if (!tag.contains("dim")) {
                    player.sendSystemMessage(Component.translatable("msg.villicense.dim.absent"));
                    return InteractionResult.SUCCESS;
                }
                if (link(player, villager, BlockPos.of(posRaw), tag.getString("dim"))) {
                    tag.remove("pos");
                    tag.remove("entity_id");
                    tag.remove("dim");
                    tag.remove("confirm");
                }
            } else {
                var entity_id = villager.getId();
                tag.putInt("entity_id", entity_id);
                tag.putString("dim", player.level().dimension().location().toString());
                player.sendSystemMessage(Component.translatable("msg.villicense.villager.set", entity_id));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
        if (level == null) return;
        final var tag = itemStack.getOrCreateTag();
        if (tag.contains("pos")) {
            final BlockPos pos = BlockPos.of(tag.getLong("pos"));
            list.add(Component.translatable("tooltip.villicense.block.set",
                    Component.translatable(
                            "format.villicense.name_pos",
                            level.getBlockState(pos).getBlock().getName(),
                            Component.translatable(
                                    "chat.coordinates",
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ()
                            ).withStyle(ChatFormatting.GREEN))
                    )
            );
        } else {
            list.add(Component.translatable("tooltip.villicense.block.not_set")
                    .withStyle(ChatFormatting.GRAY));
        }
        final boolean hasEntity = tag.contains("entity_id");
        list.add(Component.translatable(
                hasEntity ?
                        "tooltip.villicense.villager.set" :
                        "tooltip.villicense.villager.not_set"
                ).withStyle(hasEntity ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        for (int i = 0; i < 5; i++) {
            list.add(Component.translatable("tooltip.villicense.%s".formatted(String.valueOf(i)), VillicenseClient.getName4CrouchingKey()));
        }
        super.appendHoverText(itemStack, level, list, tooltipFlag);
    }

    @Override
    public InteractionResult useOn(UseOnContext useOnContext) {
        if (useOnContext.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        final Level level = useOnContext.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;
        final Player player = useOnContext.getPlayer();
        if (player == null) return InteractionResult.PASS;
        final ItemStack stack = useOnContext.getPlayer().getMainHandItem();
        if (player.isShiftKeyDown()) {
            var tag = stack.getOrCreateTag();
            tag.remove("confirm");
            final var pos = useOnContext.getClickedPos();
            if (!isValidBlock(level, pos)) {
                player.sendSystemMessage(Component.translatable("msg.villicense.block.invalid"));
                return InteractionResult.SUCCESS;
            } else {
                if (tag.contains("entity_id")) {
                    if (!tag.contains("dim")) {
                        player.sendSystemMessage(Component.translatable("msg.villicense.dim.absent"));
                        return InteractionResult.SUCCESS;
                    }
                    final String dim = tag.getString("dim");
                    final String currentDim = level.dimension().location().toString();
                    if (!dim.equals(currentDim)) {
                        player.sendSystemMessage(Component.translatable("msg.villicense.dim.different", dim, currentDim));
                        return InteractionResult.SUCCESS;
                    }
                    final int entityId = tag.getInt("entity_id");
                    final Entity e = level.getEntity(entityId);
                    if (e instanceof Villager v) {
                        if (link(player, v, pos, dim)) {
                            tag.remove("pos");
                            tag.remove("entity_id");
                            tag.remove("dim");
                            tag.remove("confirm");
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
                tag.putLong("pos", pos.asLong());
                tag.putString("dim", level.dimension().location().toString());
                player.sendSystemMessage(
                        Component.translatable(
                                "msg.villicense.block.set",
                                level.getBlockState(pos).getBlock().getName(),
                                Component.translatable(
                                        "chat.coordinates",
                                        pos.getX(), pos.getY(), pos.getZ())
                        )
                );
            }
        } else {
            final var tag = stack.getOrCreateTag();
            var confirm = stack.getOrCreateTag().getBoolean("confirm");
            if (confirm) {
                tag.remove("pos");
                tag.remove("entity_id");
                tag.remove("dim");
                tag.remove("confirm");
                player.sendSystemMessage(Component.translatable("msg.villicense.cancelled"));
            } else {
                tag.putBoolean("confirm", true);
                player.sendSystemMessage(Component.translatable("msg.villicense.cancellation.confirm"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static List<MutableComponent> genVillagerInfo(Villager villager) {
        final List<MutableComponent> list = Lists.newArrayList();
        list.add(Component.translatable("msg.villicense.villager.info.title"));
        list.add(Component.translatable("msg.villicense.villager.info.uuid", villager.getUUID().toString()));
        list.add(Component.translatable("msg.villicense.villager.info.profession", Component.translatable(
                villager.getType().getDescriptionId() + "." + BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).getPath()
        )));
        list.add(Component.translatable("msg.villicense.villager.info.job_site", genVillagerMemInfo(villager, MemoryModuleType.JOB_SITE)));
        list.add(Component.translatable("msg.villicense.villager.info.bed", genVillagerMemInfo(villager, MemoryModuleType.HOME)));
        return list;
    }

    private static MutableComponent genVillagerMemInfo(Villager villager, MemoryModuleType<GlobalPos> memModule) {
        var mem = villager.getBrain().getMemory(memModule);
        if (mem.isEmpty()) return Component.translatable("msg.villicense.none");
        final BlockPos pos = mem.get().pos();
        final Component posComp = Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
        return Component.translatable(
                "format.villicense.name_pos",
                villager.level().getBlockState(pos).getBlock().getName(),
                posComp
        );
    }

    private static boolean link(Player player, Villager villager, BlockPos pos, String dim) {
        if (!(player.level() instanceof ServerLevel level)) return false;
        final String currentDim = level.dimension().location().toString();
        if (!dim.equals(currentDim)) {
            player.sendSystemMessage(Component.translatable("msg.villicense.dim.different", dim, currentDim));
            return false;
        }
        if (!isValidBlock(level, pos)) {
            player.sendSystemMessage(Component.translatable(
                    "msg.villicense.block.may_have_changed",
                    level.getBlockState(pos).getBlock().getName()
            ));
            return false;
        }
        final BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.BEDS)) {
            villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), pos));
            villager.refreshBrain(level);
            player.sendSystemMessage(Component.translatable(
                    "msg.villicense.bed.set",
                    Component.translatable(
                            "chat.coordinates",
                            pos.getX(),
                            pos.getY(),
                            pos.getZ()
                    ))
            );
            return true;
        } else {
            final var poi = PoiTypes.forState(state);
            if (poi.isEmpty()) return false;
            final AtomicBoolean ab = new AtomicBoolean(false);
            BuiltInRegistries.VILLAGER_PROFESSION.stream()
                    .filter(p -> p.heldJobSite().test(poi.get()))
                    .findFirst()
                    .ifPresent(p -> {
                        villager.setVillagerData(villager.getVillagerData().setProfession(p));
                        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), pos));
                        villager.refreshBrain(level);
                        ab.set(true);
                        player.sendSystemMessage(Component.translatable("msg.villicense.profession.set", Component.translatable(
                                villager.getType().getDescriptionId() + "." + BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).getPath()
                        )));
                    });
            return ab.get();
        }
    }

    private static boolean isValidBlock(Level level, BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.BEDS)) return true;
        final var poi = PoiTypes.forState(state);
        if (poi.isEmpty()) return false;
        final var rk = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getResourceKey(poi.get().value());
        return rk.filter(poiTypeResourceKey -> getAvailablePoiTypes().contains(poiTypeResourceKey)).isPresent();
    }

    private static List<ResourceKey<PoiType>> a_poi;

    public static List<ResourceKey<PoiType>> getAvailablePoiTypes() {
        if (a_poi == null) {
            final List<ResourceKey<PoiType>> list = Lists.newArrayList();
            BuiltInRegistries.POINT_OF_INTEREST_TYPE
                    .stream().filter(p -> BuiltInRegistries.VILLAGER_PROFESSION
                            .stream().anyMatch(v -> v.heldJobSite()
                                    .test(BuiltInRegistries.POINT_OF_INTEREST_TYPE.wrapAsHolder(p))))
                    .forEach(p -> BuiltInRegistries.POINT_OF_INTEREST_TYPE.getResourceKey(p).ifPresent(list::add));
            a_poi = list;
        }
        return a_poi;
    }
}
