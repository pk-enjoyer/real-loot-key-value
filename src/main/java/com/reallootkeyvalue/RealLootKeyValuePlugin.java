package com.reallootkeyvalue;

import javax.inject.Inject;
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
}
