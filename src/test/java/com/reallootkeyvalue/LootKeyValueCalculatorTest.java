package com.reallootkeyvalue;

import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LootKeyValueCalculatorTest
{
	private LootKeyValueCalculator calculator;

	@Before
	public void setUp()
	{
		calculator = new LootKeyValueCalculator();
	}

	@Test
	public void identifiesLootKeyItems()
	{
		assertTrue(calculator.isLootKeyItem(ItemID.WILDY_LOOT_KEY0));
		assertTrue(calculator.isLootKeyItem(ItemID.WILDY_LOOT_KEY1));
		assertTrue(calculator.isLootKeyItem(ItemID.WILDY_LOOT_KEY2));
		assertTrue(calculator.isLootKeyItem(ItemID.WILDY_LOOT_KEY3));
		assertTrue(calculator.isLootKeyItem(ItemID.WILDY_LOOT_KEY4));
		assertFalse(calculator.isLootKeyItem(ItemID.COINS));
	}

	@Test
	public void mapsLootKeySlotsToMatchingContainers()
	{
		assertEquals(InventoryID.DEADMAN_LOOT_INV0, calculator.containerIdForKeySlot(0));
		assertEquals(InventoryID.DEADMAN_LOOT_INV1, calculator.containerIdForKeySlot(1));
		assertEquals(InventoryID.DEADMAN_LOOT_INV2, calculator.containerIdForKeySlot(2));
		assertEquals(InventoryID.DEADMAN_LOOT_INV3, calculator.containerIdForKeySlot(3));
		assertEquals(InventoryID.DEADMAN_LOOT_INV4, calculator.containerIdForKeySlot(4));
	}

	@Test
	public void mapsLootKeyContainersToMatchingSlots()
	{
		assertEquals(0, calculator.keySlotForContainerId(InventoryID.DEADMAN_LOOT_INV0));
		assertEquals(1, calculator.keySlotForContainerId(InventoryID.DEADMAN_LOOT_INV1));
		assertEquals(2, calculator.keySlotForContainerId(InventoryID.DEADMAN_LOOT_INV2));
		assertEquals(3, calculator.keySlotForContainerId(InventoryID.DEADMAN_LOOT_INV3));
		assertEquals(4, calculator.keySlotForContainerId(InventoryID.DEADMAN_LOOT_INV4));
	}

	@Test
	public void returnsNegativeContainerForInvalidLootKeySlots()
	{
		assertEquals(-1, calculator.containerIdForKeySlot(-1));
		assertEquals(-1, calculator.containerIdForKeySlot(5));
		assertEquals(-1, calculator.keySlotForContainerId(InventoryID.INV));
	}

	@Test
	public void mapsLootKeyHorizontalPositionsToContainers()
	{
		assertEquals(InventoryID.DEADMAN_LOOT_INV0, calculator.containerIdForKeyPosition(184, 164, 53));
		assertEquals(InventoryID.DEADMAN_LOOT_INV1, calculator.containerIdForKeyPosition(237, 164, 53));
		assertEquals(InventoryID.DEADMAN_LOOT_INV2, calculator.containerIdForKeyPosition(290, 164, 53));
		assertEquals(InventoryID.DEADMAN_LOOT_INV3, calculator.containerIdForKeyPosition(343, 164, 53));
		assertEquals(InventoryID.DEADMAN_LOOT_INV4, calculator.containerIdForKeyPosition(396, 164, 53));
	}

	@Test
	public void returnsNegativeContainerForInvalidLootKeyPositions()
	{
		assertEquals(-1, calculator.containerIdForKeyPosition(100, 164, 53));
		assertEquals(-1, calculator.containerIdForKeyPosition(184, 164, 0));
	}

	@Test
	public void gpOnlyKeyUsesCoinQuantityAsValue()
	{
		ItemContainer container = container(
			new Item(ItemID.COINS, 1_229_000)
		);

		long value = calculator.calculateGeValue(container, itemId -> {
			throw new AssertionError("Coins should not require a price lookup");
		});

		assertEquals(1_229_000L, value);
	}

	@Test
	public void deadmanCoinsUseQuantityAsValue()
	{
		ItemContainer container = container(
			new Item(ItemID.DEADMAN_COINS, 250_000)
		);

		long value = calculator.calculateGeValue(container, itemId -> 0);

		assertEquals(250_000L, value);
	}

	@Test
	public void mixedItemsUseGePricesTimesQuantity()
	{
		final int pricedItem = ItemID.ABYSSAL_WHIP;
		final int unpricedItem = ItemID.BRONZE_SWORD;
		ItemContainer container = container(
			new Item(ItemID.COINS, 100_000),
			new Item(pricedItem, 2),
			new Item(unpricedItem, 5)
		);

		long value = calculator.calculateGeValue(container, itemId -> {
			if (itemId == pricedItem)
			{
				return 1_500_000;
			}
			return 0;
		});

		assertEquals(3_100_000L, value);
	}

	@Test
	public void invalidAndNegativePricesDoNotContribute()
	{
		ItemContainer container = container(
			new Item(-1, 1),
			new Item(ItemID.ABYSSAL_WHIP, 1),
			new Item(ItemID.BRONZE_SWORD, 0)
		);

		long value = calculator.calculateGeValue(container, itemId -> -500);

		assertEquals(0L, value);
	}

	@Test
	public void nullAndEmptyContainersHaveNoItems()
	{
		assertFalse(calculator.hasItems(null));
		assertFalse(calculator.hasItems(container(new Item(-1, 0))));
		assertTrue(calculator.hasItems(container(new Item(ItemID.COINS, 1))));
	}

	@Test
	public void nullContainerCalculatesZero()
	{
		assertEquals(0L, calculator.calculateGeValue(null, itemId -> 1));
	}

	private static ItemContainer container(Item... items)
	{
		ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(items);
		return container;
	}
}
