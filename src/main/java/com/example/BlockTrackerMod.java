package com.example;

import net.fabricmc.api.ModInitializer;

public class BlockTrackerMod implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("BlockTracker Mod initialized!");
        EventHandlers.register();
    }
}
