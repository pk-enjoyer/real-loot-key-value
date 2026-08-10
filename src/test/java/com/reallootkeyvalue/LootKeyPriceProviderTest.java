/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LootKeyPriceProviderTest
{
	private final ItemManager itemManager = mock(ItemManager.class);
	private final RealLootKeyValueConfig config = mock(RealLootKeyValueConfig.class);
	private LootKeyPriceProvider priceProvider;

	@Before
	public void setUp()
	{
		priceProvider = new LootKeyPriceProvider(itemManager, config);
	}

	@Test
	public void runeLiteSourceUsesActivelyTradedPrice()
	{
		when(config.priceSource()).thenReturn(LootKeyPriceSource.RUNELITE);
		when(itemManager.getItemPriceWithSource(ItemID.ABYSSAL_WHIP, true)).thenReturn(1_500_000);

		assertEquals(1_500_000, priceProvider.getGePrice(ItemID.ABYSSAL_WHIP));
		verify(itemManager, never()).getItemPriceWithSource(ItemID.ABYSSAL_WHIP, false);
	}

	@Test
	public void jagexSourceUsesJagexGuidePrice()
	{
		when(config.priceSource()).thenReturn(LootKeyPriceSource.JAGEX);
		when(itemManager.getItemPriceWithSource(ItemID.ABYSSAL_WHIP, false)).thenReturn(1_600_000);

		assertEquals(1_600_000, priceProvider.getGePrice(ItemID.ABYSSAL_WHIP));
		verify(itemManager, never()).getItemPriceWithSource(ItemID.ABYSSAL_WHIP, true);
	}
}
