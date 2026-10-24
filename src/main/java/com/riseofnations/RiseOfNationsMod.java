package com.riseofnations;

import com.riseofnations.core.config.RonSettings;
import com.riseofnations.core.story.DefaultStory;
import com.riseofnations.core.story.StoryDefinition;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point. Kept deliberately small: it loads the config, checks that the built-in story is
 * valid, and registers one read-only command. All real game logic lives in com.riseofnations.core,
 * which has no Minecraft dependency and is covered by tests.
 *
 * STAGE 1 STATUS: this file was written against Minecraft 26.3 but has NOT been compiled yet.
 */
public final class RiseOfNationsMod implements ModInitializer {

    public static final String MOD_ID = "riseofnations";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static RonSettings settings = new RonSettings();

    public static RonSettings settings() {
        return settings;
    }

    @Override
    public void onInitialize() {
        settings = ConfigLoader.load(FabricLoader.getInstance().getConfigDir().resolve("riseofnations.json"));

        try {
            StoryDefinition story = DefaultStory.create();
            LOGGER.info("Rise of Nations: story loaded ({} chapters, {} quests).", story.chapters().size(), story.quests().size());
        } catch (RuntimeException e) {
            LOGGER.error("Rise of Nations: the built-in story is invalid and was not loaded.", e);
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("ron")
                        .then(Commands.literal("about")
                                .executes(context -> {
                                    String version = FabricLoader.getInstance().getModContainer(MOD_ID)
                                            .map(c -> c.getMetadata().getVersion().getFriendlyString())
                                            .orElse("unknown");
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Rise of Nations: AI Civilization v" + version + " (early development)"),
                                            false);
                                    return 1;
                                }))));

        LOGGER.info("Rise of Nations initialized.");
    }
}
