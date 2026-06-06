package com.reallootkeyvalue;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		overlay.onWidgetLoaded(event);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		overlay.onItemContainerChanged(event);
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
