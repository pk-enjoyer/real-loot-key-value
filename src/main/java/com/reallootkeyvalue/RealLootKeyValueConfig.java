package com.reallootkeyvalue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("realLootKeyValue")
public interface RealLootKeyValueConfig extends Config
{
	@ConfigItem(
		keyName = "valueDisplayMode",
		name = "Value display",
		description = "Configures where the real Grand Exchange value is displayed in the loot key chest."
	)
	default ValueDisplayMode valueDisplayMode()
	{
		return ValueDisplayMode.COMPACT_KEY_TAB;
	}
}
