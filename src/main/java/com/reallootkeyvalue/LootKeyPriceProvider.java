/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemManager;

@Singleton
class LootKeyPriceProvider implements LootKeyValueCalculator.PriceLookup
{
	private final ItemManager itemManager;
	private final RealLootKeyValueConfig config;

	@Inject
	LootKeyPriceProvider(ItemManager itemManager, RealLootKeyValueConfig config)
	{
		this.itemManager = itemManager;
		this.config = config;
	}

	@Override
	public int getGePrice(int itemId)
	{
		if (config.priceSource() == LootKeyPriceSource.JAGEX)
		{
			return itemManager.getItemPriceWithSource(itemId, false);
		}

		return itemManager.getItemPriceWithSource(itemId, true);
	}
}
