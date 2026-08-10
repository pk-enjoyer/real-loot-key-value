/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("realLootKeyValue")
@SuppressWarnings("SameReturnValue")
public interface RealLootKeyValueConfig extends Config
{
	@ConfigItem(
		keyName = "priceSource",
		name = "Price source",
		description = "RuneLite actively traded uses Wiki prices with RuneLite safeguards. Jagex guide price uses the in-game Grand Exchange guide prices.",
		position = 0
	)
	default LootKeyPriceSource priceSource()
	{
		return LootKeyPriceSource.RUNELITE;
	}

	@ConfigItem(
		keyName = "showCompactKeyTabValue",
		name = "Compact key tab value",
		description = "Shows the selected Grand Exchange value directly on the loot key tab.",
		position = 1
	)
	default boolean showCompactKeyTabValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showKeyTabIcon",
		name = "Hide key tab text",
		description = "Removes the original loot key tab text when the compact key tab value is disabled.",
		position = 2
	)
	default boolean showKeyTabIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBottomText",
		name = "Bottom text",
		description = "Shows the selected Grand Exchange value at the bottom of the loot key chest.",
		position = 3
	)
	default boolean showBottomText()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTopRightText",
		name = "Top right text",
		description = "Shows the selected Grand Exchange value in the top-right corner of the loot key chest.",
		position = 4
	)
	default boolean showTopRightText()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightHighValueText",
		name = "Highlight high values",
		description = "Colors values of 10m gp or more green.",
		position = 5
	)
	default boolean highlightHighValueText()
	{
		return true;
	}
}
