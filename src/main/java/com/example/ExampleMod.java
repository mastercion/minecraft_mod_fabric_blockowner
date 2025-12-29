package com.example;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mojang.brigadier.CommandDispatcher;

import javax.tools.Tool;

public class ExampleMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("BlockOwner");
	private static final Config config = Config.getInstance();
	public static final String VERSION;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.



		ExampleMod.logInfo("-----------------------------");
		ExampleMod.logInfo("BlockTracker " + VERSION + " started.");
		ExampleMod.logInfo("Log Level: "+ config.getLogLevel());
		ExampleMod.logInfo("-----------------------------");

		EventHandlers.register();
		LogLevelCommand.register();
		ToolCommand.register();
		MessageStyle.register();
		PlayerBlockList.register();
		BlockDisplayVisualizer.register();
		PermissionCommand.register();
		HighlightManager.register();
	}

	public static void logInfo(String message) {
		LOGGER.info("[BlockOwner] " + message);
	}

	static {
		ModMetadata metadata = FabricLoader.getInstance().getModContainer("modid").get().getMetadata();
		VERSION = metadata.getVersion().getFriendlyString();
	}
}