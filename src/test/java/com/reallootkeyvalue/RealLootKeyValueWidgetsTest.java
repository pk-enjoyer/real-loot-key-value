/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RealLootKeyValueWidgetsTest
{
	private final LootKeyValueCalculator calculator = new LootKeyValueCalculator();
	private final AtomicReference<ItemContainer> firstKeyContainer = new AtomicReference<>();
	private final AtomicReference<Boolean> showCompactKeyTabValue = new AtomicReference<>(true);
	private final AtomicReference<Boolean> showKeyTabIcon = new AtomicReference<>(true);
	private final AtomicReference<Boolean> highlightHighValueText = new AtomicReference<>(true);

	private RealLootKeyValueWidgets widgets;
	private Widget firstTabText;

	private static Widget textWidget(int id, String initialText)
	{
		final Widget widget = mock(Widget.class);
		final AtomicReference<String> text = new AtomicReference<>(initialText);
		final AtomicInteger textColor = new AtomicInteger(0xff981f);

		when(widget.getId()).thenReturn(id);
		when(widget.getIndex()).thenReturn(id);
		when(widget.getBounds()).thenReturn(new Rectangle());
		when(widget.getText()).thenAnswer(invocation -> text.get());
		when(widget.setText(anyString())).thenAnswer(invocation -> {
			text.set(invocation.getArgument(0, String.class));
			return widget;
		});
		when(widget.getTextColor()).thenAnswer(invocation -> textColor.get());
		when(widget.setTextColor(anyInt())).thenAnswer(invocation -> {
			textColor.set(invocation.getArgument(0, Integer.class));
			return widget;
		});
		return widget;
	}

	private static ItemContainer container(Item... items)
	{
		final ItemContainer container = mock(ItemContainer.class);
		when(container.getItems()).thenReturn(items);
		return container;
	}

	@Before
	public void setUp()
	{
		final Client client = mock(Client.class);
		final ClientThread clientThread = mock(ClientThread.class);
		final LootKeyPriceProvider priceProvider = mock(LootKeyPriceProvider.class);
		final RealLootKeyValueConfig config = mock(RealLootKeyValueConfig.class);

		doAnswer(invocation -> {
			invocation.getArgument(0, Runnable.class).run();
			return null;
		}).when(clientThread).invoke(any(Runnable.class));

		when(config.showCompactKeyTabValue()).thenAnswer(invocation -> showCompactKeyTabValue.get());
		when(config.showKeyTabIcon()).thenAnswer(invocation -> showKeyTabIcon.get());
		when(config.highlightHighValueText()).thenAnswer(invocation -> highlightHighValueText.get());

		final Widget tabs = mock(Widget.class);
		final Widget[] children = tabChildren();
		when(tabs.isHidden()).thenReturn(false);
		when(tabs.getChildren()).thenReturn(children);
		when(client.getWidget(InterfaceID.WildyLootChest.TABS)).thenReturn(tabs);
		when(client.getItemContainer(anyInt())).thenAnswer(invocation -> {
			if (invocation.getArgument(0, Integer.class) == calculator.containerIdForKeySlot(0))
			{
				return firstKeyContainer.get();
			}
			return null;
		});

		widgets = new RealLootKeyValueWidgets(client, clientThread, priceProvider, calculator, config);
	}

	@Test
	public void compactValueClearsConsumedKeyWithoutRestoringOldTabText()
	{
		firstKeyContainer.set(container(new Item(ItemID.COINS, 1_229_000)));

		widgets.startUp();

		assertEquals("1229k", firstTabText.getText());

		firstKeyContainer.set(null);

		widgets.startUp();

		assertEquals("", firstTabText.getText());

		showCompactKeyTabValue.set(false);
		showKeyTabIcon.set(false);

		widgets.startUp();

		assertEquals("", firstTabText.getText());
	}

	@Test
	public void inactiveCompactValueRestoresLiveKeyOriginalText()
	{
		firstKeyContainer.set(container(new Item(ItemID.COINS, 1_229_000)));

		widgets.startUp();

		assertEquals("1229k", firstTabText.getText());

		showCompactKeyTabValue.set(false);
		showKeyTabIcon.set(false);

		widgets.startUp();

		assertEquals("1", firstTabText.getText());
	}

	private Widget[] tabChildren()
	{
		final Widget[] children = new Widget[15];
		for (int i = 0; i < children.length; i++)
		{
			children[i] = textWidget(i, "");
		}

		for (int slot = 0; slot < 5; slot++)
		{
			children[10 + slot] = textWidget(10 + slot, Integer.toString(slot + 1));
		}
		firstTabText = children[10];
		return children;
	}
}
