package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class HighlightManager {

    private static final List<HighlightEntry> activeHighlights = new ArrayList<>();
    private static final int HIGHLIGHT_DURATION_TICKS = 100;
    private static final String HIGHLIGHT_TAG = "tracker_glow";

    public static void highlightBlock(ServerWorld world, BlockPos pos) {
        removeHighlightAt(pos);

        BlockState state = world.getBlockState(pos);
        if (!state.isAir()) {
        } else {
            state = Blocks.GLASS.getDefaultState();
        }

        DisplayEntity.BlockDisplayEntity entity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
        entity.setPos(pos.getX(), pos.getY(), pos.getZ());
        entity.setBlockState(state);

        float scale = 1.02f;
        float offset = (1.0f - scale) / 2.0f;
        entity.setTransformation(new AffineTransformation(
                new Vector3f(offset, offset, offset),
                null,
                new Vector3f(scale, scale, scale),
                null
        ));


        entity.setBrightness(new Brightness(15, 15));
        entity.setGlowing(true);
        entity.setViewRange(64.0f);
        entity.addCommandTag(HIGHLIGHT_TAG);

        if (world.spawnEntity(entity)) {
            activeHighlights.add(new HighlightEntry(entity.getUuid(), world, pos, HIGHLIGHT_DURATION_TICKS));
            LoggerUtil.log("Highlight spawned at " + pos.toShortString(), LoggerUtil.LogLevel.ALL);
        }
    }

    private static void removeHighlightAt(BlockPos pos) {
        Iterator<HighlightEntry> iterator = activeHighlights.iterator();
        while (iterator.hasNext()) {
            HighlightEntry entry = iterator.next();
            if (entry.pos.equals(pos)) {
                Entity entity = entry.world.getEntity(entry.uuid);
                if (entity != null) {
                    entity.setGlowing(false);
                    entity.discard();
                    LoggerUtil.log("Highlight overwritten at " + pos.toShortString(), LoggerUtil.LogLevel.ALL);
                }
                iterator.remove();
            }
        }
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (activeHighlights.isEmpty()) return;

            Iterator<HighlightEntry> iterator = activeHighlights.iterator();
            while (iterator.hasNext()) {
                HighlightEntry entry = iterator.next();
                entry.ticksLeft--;

                if (entry.ticksLeft <= 0) {
                    Entity entity = entry.world.getEntity(entry.uuid);
                    if (entity != null) {
                        entity.setGlowing(false);
                        entity.discard();
                        iterator.remove();
                        LoggerUtil.log("Highlight expired at " + entry.pos.toShortString(), LoggerUtil.LogLevel.ALL);
                    } else {
                        boolean isChunkLoaded = entry.world.isChunkLoaded(entry.pos.getX() >> 4, entry.pos.getZ() >> 4);
                        if (isChunkLoaded) {
                            iterator.remove();
                            LoggerUtil.log("Highlight stopped tracking (Missing) at " + entry.pos.toShortString(), LoggerUtil.LogLevel.ALL);
                        }
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            activeHighlights.clear();
        });
    }

    private static class HighlightEntry {
        UUID uuid;
        ServerWorld world;
        BlockPos pos;
        int ticksLeft;

        public HighlightEntry(UUID uuid, ServerWorld world, BlockPos pos, int ticksLeft) {
            this.uuid = uuid;
            this.world = world;
            this.pos = pos;
            this.ticksLeft = ticksLeft;
        }
    }
}