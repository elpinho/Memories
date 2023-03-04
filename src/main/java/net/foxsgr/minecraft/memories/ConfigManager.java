package net.foxsgr.minecraft.memories;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Manages the mod's configuration.
 */
public class ConfigManager {
    public static class ClientConfig {
        /**
         * The URL to which the screenshots will be uploaded.
         */
        public final ForgeConfigSpec.ConfigValue<String> url;

        /**
         * Creates a new client configuration.
         *
         * @param builder the builder to use.
         */
        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("client");

            url = builder
                    .comment("The URL to which the screenshots will be uploaded.")
                    .define("url", "https://example.com/api/screenshot");

            builder.pop();
        }
    }

    /**
     * The client configuration spec.
     */
    public static final ForgeConfigSpec clientConfigSpec;

    /**
     * The client configuration.
     */
    public static final ClientConfig clientConfig;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        clientConfig = new ClientConfig(builder);
        clientConfigSpec = builder.build();
    }

    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientConfigSpec);
    }
}
