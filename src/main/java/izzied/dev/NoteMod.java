package izzied.dev;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoteMod implements ModInitializer {
	public static final String MOD_ID = "notemod";
	public static final String DISPLAY_NAME = "Notes";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("{} initialized", DISPLAY_NAME);
	}
}
