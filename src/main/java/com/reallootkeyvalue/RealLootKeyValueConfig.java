package com.reallootkeyvalue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("realLootKeyValue")
public interface RealLootKeyValueConfig extends Config
{
	@ConfigItem(
		keyName = "showCompactKeyTabValue",
		name = "Compact key tab value",
		description = "Shows the real Grand Exchange value directly on the loot key tab.",
		position = 0
	)
	default boolean showCompactKeyTabValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showKeyTabIcon",
		name = "Key tab icon",
		description = "Shows the loot key icon on the key tab when the compact key tab value is disabled.",
		position = 1
	)
	default boolean showKeyTabIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBottomText",
		name = "Bottom text",
		description = "Shows the real Grand Exchange value at the bottom of the loot key chest.",
		position = 2
	)
	default boolean showBottomText()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTopRightText",
		name = "Top right text",
		description = "Shows the real Grand Exchange value in the top-right corner of the loot key chest.",
		position = 3
	)
	default boolean showTopRightText()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightHighValueText",
		name = "Highlight high values",
		description = "Colors values of 10m gp or more green.",
		position = 4
	)
	default boolean highlightHighValueText()
	{
		return true;
	}
}
