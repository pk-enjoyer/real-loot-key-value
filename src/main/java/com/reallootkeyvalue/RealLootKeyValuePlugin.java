/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Real Loot Key Value",
	description = "Shows the real Grand Exchange value of PvP loot keys in the loot chest interface.",
	tags = {"loot", "key", "value", "pvp", "wilderness"}
)
public class RealLootKeyValuePlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RealLootKeyValueOverlay overlay;

	@Inject
	private RealLootKeyValueWidgets widgets;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		widgets.startUp();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		widgets.shutDown();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		overlay.onWidgetLoaded(event);
		widgets.onWidgetLoaded(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		widgets.onConfigChanged(event);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		widgets.onItemContainerChanged(event);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		overlay.onMenuOpened(event);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		overlay.onMenuOptionClicked(event);
	}

	@Provides
	RealLootKeyValueConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RealLootKeyValueConfig.class);
	}
}
