/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import java.util.Arrays;
import javax.inject.Singleton;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;

@Singleton
class LootKeyValueCalculator
{
	private static final int[] KEY_ITEM_IDS = {
		ItemID.WILDY_LOOT_KEY0,
		ItemID.WILDY_LOOT_KEY1,
		ItemID.WILDY_LOOT_KEY2,
		ItemID.WILDY_LOOT_KEY3,
		ItemID.WILDY_LOOT_KEY4
	};

	private static final int[] CONTAINER_IDS = {
		InventoryID.DEADMAN_LOOT_INV0,
		InventoryID.DEADMAN_LOOT_INV1,
		InventoryID.DEADMAN_LOOT_INV2,
		InventoryID.DEADMAN_LOOT_INV3,
		InventoryID.DEADMAN_LOOT_INV4
	};

	private static final int[] COIN_ITEM_IDS = {
		ItemID.COINS,
		ItemID.COINS_2,
		ItemID.COINS_3,
		ItemID.COINS_4,
		ItemID.COINS_5,
		ItemID.COINS_25,
		ItemID.COINS_100,
		ItemID.COINS_250,
		ItemID.COINS_1000,
		ItemID.COINS_10000,
		ItemID.DEADMAN_COINS,
		ItemID.DEADMAN_COINS_2,
		ItemID.DEADMAN_COINS_3,
		ItemID.DEADMAN_COINS_4,
		ItemID.DEADMAN_COINS_5,
		ItemID.DEADMAN_COINS_25,
		ItemID.DEADMAN_COINS_100,
		ItemID.DEADMAN_COINS_250,
		ItemID.DEADMAN_COINS_1000,
		ItemID.DEADMAN_COINS_10000
	};

	boolean isLootKeyItem(int itemId)
	{
		for (int keyItemId : KEY_ITEM_IDS)
		{
			if (keyItemId == itemId)
			{
				return true;
			}
		}

		return false;
	}

	int containerIdForKeySlot(int slot)
	{
		if (slot < 0 || slot >= CONTAINER_IDS.length)
		{
			return -1;
		}

		return CONTAINER_IDS[slot];
	}

	int keySlotForContainerId(int containerId)
	{
		for (int slot = 0; slot < CONTAINER_IDS.length; slot++)
		{
			if (CONTAINER_IDS[slot] == containerId)
			{
				return slot;
			}
		}

		return -1;
	}

	int keySlotForPosition(int itemX, int parentX, int slotPitch)
	{
		if (slotPitch <= 0)
		{
			return -1;
		}

		final int relativeX = itemX - parentX;
		if (relativeX < 0)
		{
			return -1;
		}

		final int slot = Math.round((float) relativeX / slotPitch);
		return containerIdForKeySlot(slot) >= 0 ? slot : -1;
	}

	boolean hasItems(ItemContainer container)
	{
		if (container == null)
		{
			return false;
		}

		for (Item item : container.getItems())
		{
			if (item.getId() > -1 && item.getQuantity() > 0)
			{
				return true;
			}
		}

		return false;
	}

	long calculateGeValue(ItemContainer container, PriceLookup priceLookup)
	{
		if (container == null)
		{
			return 0L;
		}

		long total = 0L;
		for (Item item : container.getItems())
		{
			final int itemId = item.getId();
			final int quantity = item.getQuantity();
			if (itemId < 0 || quantity <= 0)
			{
				continue;
			}

			if (isCoin(itemId))
			{
				total += quantity;
				continue;
			}

			final int gePrice = Math.max(0, priceLookup.getGePrice(itemId));
			total += (long) gePrice * quantity;
		}

		return total;
	}

	private boolean isCoin(int itemId)
	{
		return Arrays.stream(COIN_ITEM_IDS).anyMatch(coinItemId -> coinItemId == itemId);
	}

	interface PriceLookup
	{
		int getGePrice(int itemId);
	}
}
