package com.reallootkeyvalue;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ImageUtil;

class RealLootKeyValueOverlay extends WidgetItemOverlay
{
	private static final BufferedImage LOOT_KEY_IMAGE = ImageUtil.loadImageResource(RealLootKeyValueOverlay.class, "/com/reallootkeyvalue/loot_key.png");
	private static final Color CHEST_BACKGROUND = new Color(64, 55, 43);
	private static final Color HOVER_BACKGROUND = new Color(78, 68, 54);
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final int PATCH_X_OFFSET = 0;
	private static final int PATCH_Y_OFFSET = -1;
	private static final int PATCH_MIN_WIDTH = 35;
	private static final int ICON_Y_OFFSET = 3;
	private static final int TEXT_Y_OFFSET = 31;
	private static final int PATCH_PADDING_X = 2;
	private static final int PATCH_PADDING_BOTTOM = 3;
	private static final int KEY_SLOT_PITCH = 53;
	private static final int PATCH_CORNER_CUT = 4;
	private static final Pattern VIEW_TAB_PATTERN = Pattern.compile("(?i)\\bview\\s+tab\\s+(\\d+)\\b");

	private final Client client;
	private final ItemManager itemManager;
	private final LootKeyValueCalculator calculator;

	@Inject
	RealLootKeyValueOverlay(Client client, ItemManager itemManager, LootKeyValueCalculator calculator)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.calculator = calculator;
		showOnInterfaces(InterfaceID.WILDY_LOOT_CHEST);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!calculator.isLootKeyItem(itemId))
		{
			return;
		}

		final int keySlot = getKeySlot(widgetItem);
		final int containerId = calculator.containerIdForKeySlot(keySlot);
		if (containerId < 0)
		{
			return;
		}

		final ItemContainer container = client.getItemContainer(containerId);
		if (!calculator.hasItems(container))
		{
			return;
		}

		final long value = calculator.calculateGeValue(container, itemManager::getItemPrice);
		final String text = LootKeyValueFormatter.formatOverlayValue(value);
		renderReplacementTile(graphics, widgetItem.getCanvasBounds(), text, isTabHovered(keySlot));
	}

	private int getKeySlot(WidgetItem widgetItem)
	{
		final Widget widget = widgetItem.getWidget();
		final Widget parent = widget.getParent();
		if (parent != null)
		{
			final int byPosition = keySlotForPosition(
				widgetItem.getCanvasBounds().x,
				parent.getBounds().x);
			if (byPosition >= 0)
			{
				return byPosition;
			}
		}

		return widget.getIndex();
	}

	private int keySlotForPosition(int itemX, int parentX)
	{
		final int relativeX = itemX - parentX;
		if (relativeX < 0)
		{
			return -1;
		}

		return Math.round((float) relativeX / KEY_SLOT_PITCH);
	}

	private boolean isTabHovered(int keySlot)
	{
		if (keySlot < 0)
		{
			return false;
		}

		for (MenuEntry menuEntry : client.getMenuEntries())
		{
			if (calculator.isLootKeyItem(menuEntry.getItemId()) && menuEntry.getWidget() != null && menuEntry.getWidget().getIndex() == keySlot)
			{
				return true;
			}

			final int viewTab = parseViewTab(menuEntry.getOption(), menuEntry.getTarget());
			if (viewTab == keySlot)
			{
				return true;
			}
		}

		return false;
	}

	private int parseViewTab(String option, String target)
	{
		final String menuText = stripTags(option) + " " + stripTags(target);
		final Matcher matcher = VIEW_TAB_PATTERN.matcher(menuText);
		if (!matcher.find())
		{
			return -1;
		}

		try
		{
			return Integer.parseInt(matcher.group(1));
		}
		catch (NumberFormatException ex)
		{
			return -1;
		}
	}

	private String stripTags(String value)
	{
		return value == null ? "" : value.replaceAll("<[^>]*>", "");
	}

	private void renderReplacementTile(Graphics2D graphics, Rectangle itemBounds, String text, boolean hovered)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());

		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(text);
		final int patchWidth = Math.max(PATCH_MIN_WIDTH, textWidth + (PATCH_PADDING_X * 2));
		final int patchX = itemBounds.x + PATCH_X_OFFSET;
		final int patchY = itemBounds.y + PATCH_Y_OFFSET;
		final int iconX = patchX + ((patchWidth - LOOT_KEY_IMAGE.getWidth()) / 2);
		final int iconY = itemBounds.y + ICON_Y_OFFSET;
		final int textX = patchX + ((patchWidth - textWidth) / 2);
		final int textBaselineY = itemBounds.y + TEXT_Y_OFFSET;
		final int patchHeight = (textBaselineY - patchY) + metrics.getDescent() + PATCH_PADDING_BOTTOM;

		graphics.setColor(hovered ? HOVER_BACKGROUND : CHEST_BACKGROUND);
		graphics.fillPolygon(createTabPatch(patchX, patchY, patchWidth, patchHeight));
		graphics.drawImage(LOOT_KEY_IMAGE, iconX, iconY, null);

		final TextComponent textComponent = new TextComponent();
		textComponent.setPosition(new Point(textX, textBaselineY));
		textComponent.setText(text);
		textComponent.setColor(TEXT_COLOR);
		textComponent.setOutline(true);
		textComponent.render(graphics);
	}

	private Polygon createTabPatch(int x, int y, int width, int height)
	{
		Polygon polygon = new Polygon();
		polygon.addPoint(x, y);
		polygon.addPoint(x + width - PATCH_CORNER_CUT, y);
		polygon.addPoint(x + width, y + PATCH_CORNER_CUT);
		polygon.addPoint(x + width, y + height);
		polygon.addPoint(x, y + height);
		return polygon;
	}
}
