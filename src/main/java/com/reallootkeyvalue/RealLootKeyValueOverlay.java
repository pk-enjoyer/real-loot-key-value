/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

class RealLootKeyValueOverlay extends WidgetItemOverlay
{
	private static final Color CHEST_BACKGROUND = new Color(64, 55, 43);
	private static final Color PATCH_BORDER = new Color(38, 32, 25);
	private static final Color PATCH_BORDER_SHADOW = new Color(20, 17, 13);
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final Color HIGH_VALUE_TEXT_COLOR = new Color(13, 196, 102);
	private static final long HIGH_VALUE_TEXT_THRESHOLD = 10_000_000L;
	private static final int KEY_SLOT_PITCH = 40;
	private static final int BOTTOM_TEXT_BASELINE_Y_OFFSET = 208;
	private static final int TOP_RIGHT_TEXT_RIGHT_OFFSET = 278;
	private static final int TOP_RIGHT_TEXT_BASELINE_Y_OFFSET = 11;
	private static final int TOP_RIGHT_TEXT_LINE_GAP = 1;
	private static final int TOP_RIGHT_TEXT_PADDING_X = 3;
	private static final int TOP_RIGHT_TEXT_PADDING_Y = 2;
	private static final int PATCH_CORNER_CUT = 4;
	private static final String BOTTOM_TEXT_PREFIX = "Value in chest: ";
	private static final Pattern VIEW_TAB_PATTERN = Pattern.compile("(?i)\\bview\\s+tab\\s+(\\d+)\\b");

	private final Client client;
	private final ItemManager itemManager;
	private final LootKeyValueCalculator calculator;
	private final RealLootKeyValueConfig config;
	private Rectangle pendingBottomTextBounds;
	private int pendingBottomTextKeySlot;
	private String pendingBottomText;
	private Rectangle pendingTopRightTextBounds;
	private String pendingTopRightText;
	private Color pendingTopRightTextColor;
	private int selectedKeySlot = -1;

	@Inject
	RealLootKeyValueOverlay(Client client, ItemManager itemManager, LootKeyValueCalculator calculator, RealLootKeyValueConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.calculator = calculator;
		this.config = config;
		showOnInterfaces(InterfaceID.WILDY_LOOT_CHEST);
	}


	void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (calculator.isLootKeyItem(event.getItemId()) && event.getWidget() != null)
		{
			selectKeySlot(getKeySlot(event.getWidget()));
			return;
		}

		final int viewTab = parseViewTab(event.getMenuOption(), event.getMenuTarget());
		if (viewTab >= 0)
		{
			selectKeySlot(viewTab);
		}
	}

	void onMenuOpened(MenuOpened event)
	{
		final int menuKeySlot = getKeySlot(event.getMenuEntries());
		if (menuKeySlot >= 0)
		{
			selectKeySlot(menuKeySlot);
		}
	}

	void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.WILDY_LOOT_CHEST)
		{
			selectKeySlot(-1);
		}
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
		final Color valueTextColor = getValueTextColor(value);
		if (config.showBottomText() && shouldRenderValueTextForKey(keySlot))
		{
			queueBottomText(widgetItem, keySlot, LootKeyValueFormatter.formatChestValue(value));
		}

		if (config.showTopRightText() && shouldRenderValueTextForKey(keySlot))
		{
			queueTopRightText(widgetItem, keySlot, LootKeyValueFormatter.formatGpAmount(value), valueTextColor);
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		pendingBottomTextBounds = null;
		pendingBottomTextKeySlot = -1;
		pendingBottomText = null;
		pendingTopRightTextBounds = null;
		pendingTopRightText = null;
		pendingTopRightTextColor = TEXT_COLOR;

		if (!config.showBottomText() && !config.showTopRightText())
		{
			return null;
		}

		final int visibleKeySlot = getVisibleChestKeySlot();
		if (visibleKeySlot >= 0)
		{
			selectKeySlot(visibleKeySlot);
		}

		final Shape originalClip = graphics.getClip();
		final Dimension dimension = super.render(graphics);
		graphics.setClip(originalClip);

		if (pendingBottomTextBounds != null && pendingBottomText != null)
		{
			renderBottomText(graphics, pendingBottomTextBounds, pendingBottomTextKeySlot, pendingBottomText);
		}

		if (pendingTopRightTextBounds != null && pendingTopRightText != null)
		{
			renderTopRightText(graphics, pendingTopRightTextBounds, pendingTopRightText, pendingTopRightTextColor);
		}

		return dimension;
	}

	private Color getValueTextColor(long value)
	{
		return config.highlightHighValueText() && value >= HIGH_VALUE_TEXT_THRESHOLD ? HIGH_VALUE_TEXT_COLOR : TEXT_COLOR;
	}

	private void queueBottomText(WidgetItem widgetItem, int keySlot, String text)
	{
		pendingBottomTextBounds = widgetItem.getCanvasBounds();
		pendingBottomTextKeySlot = keySlot;
		pendingBottomText = text;
	}

	private void queueTopRightText(WidgetItem widgetItem, int keySlot, String text, Color textColor)
	{
		pendingTopRightTextBounds = getTopRightNormalizedBounds(widgetItem.getCanvasBounds(), keySlot);
		pendingTopRightText = text;
		pendingTopRightTextColor = textColor;
	}

	private Rectangle getTopRightNormalizedBounds(Rectangle bounds, int keySlot)
	{
		return new Rectangle(
			bounds.x - (Math.max(0, keySlot) * KEY_SLOT_PITCH),
			bounds.y,
			bounds.width,
			bounds.height);
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

	private boolean shouldRenderValueTextForKey(int keySlot)
	{
		if (keySlot < 0)
		{
			return false;
		}

		if (hasSelectedKeySlotItems())
		{
			return keySlot == selectedKeySlot;
		}

		return keySlot == getFirstPopulatedKeySlot();
	}

	private boolean hasSelectedKeySlotItems()
	{
		return selectedKeySlot >= 0 &&
			calculator.hasItems(client.getItemContainer(calculator.containerIdForKeySlot(selectedKeySlot)));
	}

	private int getKeySlot(MenuEntry[] menuEntries)
	{
		for (MenuEntry menuEntry : menuEntries)
		{
			if (calculator.isLootKeyItem(menuEntry.getItemId()) && menuEntry.getWidget() != null)
			{
				return getKeySlot(menuEntry.getWidget());
			}

			final int viewTab = parseViewTab(menuEntry.getOption(), menuEntry.getTarget());
			if (viewTab >= 0)
			{
				return viewTab;
			}
		}

		return -1;
	}

	private int getKeySlot(Widget widget)
	{
		final Widget parent = widget.getParent();
		if (parent != null)
		{
			final int byPosition = keySlotForPosition(widget.getBounds().x, parent.getBounds().x);
			if (byPosition >= 0)
			{
				return byPosition;
			}
		}

		return widget.getIndex();
	}

	private int getFirstPopulatedKeySlot()
	{
		for (int slot = 0; ; slot++)
		{
			final int containerId = calculator.containerIdForKeySlot(slot);
			if (containerId < 0)
			{
				return -1;
			}

			if (calculator.hasItems(client.getItemContainer(containerId)))
			{
				return slot;
			}
		}
	}

	private int getVisibleChestKeySlot()
	{
		final Widget itemsWidget = client.getWidget(InterfaceID.WildyLootChest.ITEMS);
		if (itemsWidget == null || itemsWidget.isHidden())
		{
			return -1;
		}

		final Map<Integer, Integer> visibleItems = getWidgetItems(itemsWidget);
		if (visibleItems.isEmpty())
		{
			return -1;
		}

		return getUniqueMatchingKeySlot(visibleItems);
	}

	private int getUniqueMatchingKeySlot(Map<Integer, Integer> visibleItems)
	{
		int matchingSlot = -1;
		int matchingSlotCount = 0;
		for (int slot = 0; ; slot++)
		{
			final int containerId = calculator.containerIdForKeySlot(slot);
			if (containerId < 0)
			{
				break;
			}

			if (!visibleItems.equals(getContainerItems(client.getItemContainer(containerId))))
			{
				continue;
			}

			if (slot == selectedKeySlot)
			{
				return slot;
			}

			matchingSlot = slot;
			matchingSlotCount++;
		}

		return matchingSlotCount == 1 ? matchingSlot : -1;
	}

	private Map<Integer, Integer> getWidgetItems(Widget widget)
	{
		final Map<Integer, Integer> items = new HashMap<>();
		addWidgetItem(items, widget);
		final Widget[] children = widget.getDynamicChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				addWidgetItem(items, child);
			}
		}

		return items;
	}

	private Map<Integer, Integer> getContainerItems(ItemContainer container)
	{
		final Map<Integer, Integer> items = new HashMap<>();
		if (container == null)
		{
			return items;
		}

		for (Item item : container.getItems())
		{
			addItem(items, item.getId(), item.getQuantity());
		}

		return items;
	}

	private void addWidgetItem(Map<Integer, Integer> items, Widget widget)
	{
		if (widget != null)
		{
			addItem(items, widget.getItemId(), widget.getItemQuantity());
		}
	}

	private void addItem(Map<Integer, Integer> items, int itemId, int quantity)
	{
		if (itemId > -1 && quantity > 0)
		{
			items.merge(itemId, quantity, Integer::sum);
		}
	}

	private int keySlotForPosition(int itemX, int parentX)
	{
		return calculator.keySlotForPosition(itemX, parentX, KEY_SLOT_PITCH);
	}

	private int parseViewTab(String option, String target)
	{
		final String cleanOption = stripTags(option).trim();
		final String cleanTarget = stripTags(target).trim();
		if ("View tab".equalsIgnoreCase(cleanOption))
		{
			return parseViewTabSlot(cleanTarget);
		}

		final String menuText = cleanOption + " " + cleanTarget;
		final Matcher matcher = VIEW_TAB_PATTERN.matcher(menuText);
		if (!matcher.find())
		{
			return -1;
		}

		return parseViewTabSlot(matcher.group(1));
	}

	private int parseViewTabSlot(String value)
	{
		return parseIntOrNegativeOne(value);
	}

	private int parseIntOrNegativeOne(String value)
	{
		try
		{
			return Integer.parseInt(value);
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

	private void selectKeySlot(int keySlot)
	{
		selectedKeySlot = keySlot;
	}

	private void renderBottomText(Graphics2D graphics, Rectangle bounds, int keySlot, String text)
	{
		graphics.setFont(FontManager.getRunescapeFont());
		final FontMetrics metrics = graphics.getFontMetrics();
		final int firstTabX = bounds.x - (Math.max(0, keySlot) * KEY_SLOT_PITCH);
		final String valueText = text.startsWith(BOTTOM_TEXT_PREFIX) ? text.substring(BOTTOM_TEXT_PREFIX.length()) : text;
		final int valueTextWidth = metrics.stringWidth(valueText);
		final int gpTextWidth = metrics.stringWidth(" gp") - 1;
		final int textX = firstTabX + (KEY_SLOT_PITCH * 2) + gpTextWidth - (valueTextWidth / 2);
		final int textY = bounds.y + BOTTOM_TEXT_BASELINE_Y_OFFSET;

		renderPlainText(graphics, text, textX, textY, TEXT_COLOR);
	}

	private void renderTopRightText(Graphics2D graphics, Rectangle bounds, String amountText, Color amountColor)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());
		final FontMetrics labelMetrics = graphics.getFontMetrics();
		graphics.setFont(FontManager.getRunescapeFont());
		final FontMetrics amountMetrics = graphics.getFontMetrics();
		final String labelText = "Value in chest:";
		final int blockWidth = Math.max(labelMetrics.stringWidth(labelText), amountMetrics.stringWidth(amountText));
		final int textX = bounds.x + TOP_RIGHT_TEXT_RIGHT_OFFSET - blockWidth;
		final int labelY = bounds.y + TOP_RIGHT_TEXT_BASELINE_Y_OFFSET;
		final int amountY = labelY + labelMetrics.getDescent() + amountMetrics.getAscent() + TOP_RIGHT_TEXT_LINE_GAP;
		final int patchX = textX - TOP_RIGHT_TEXT_PADDING_X;
		final int patchY = labelY - labelMetrics.getAscent() - TOP_RIGHT_TEXT_PADDING_Y;
		final int patchWidth = blockWidth + (TOP_RIGHT_TEXT_PADDING_X * 2);
		final int patchHeight = (amountY - labelY) + labelMetrics.getAscent() + amountMetrics.getDescent() + (TOP_RIGHT_TEXT_PADDING_Y * 2);

		graphics.setColor(PATCH_BORDER_SHADOW);
		graphics.drawPolygon(createCutCornerPatch(patchX + 1, patchY + 1, patchWidth, patchHeight));
		graphics.setColor(CHEST_BACKGROUND);
		graphics.fillPolygon(createCutCornerPatch(patchX, patchY, patchWidth, patchHeight));
		graphics.setColor(PATCH_BORDER);
		graphics.drawPolygon(createCutCornerPatch(patchX, patchY, patchWidth, patchHeight));

		graphics.setFont(FontManager.getRunescapeSmallFont());
		renderPlainText(graphics, labelText, textX + ((blockWidth - labelMetrics.stringWidth(labelText)) / 2), labelY, TEXT_COLOR);
		graphics.setFont(FontManager.getRunescapeFont());
		renderPlainText(graphics, amountText, textX + ((blockWidth - amountMetrics.stringWidth(amountText)) / 2), amountY, amountColor);
	}

	private void renderPlainText(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	private Polygon createCutCornerPatch(int x, int y, int width, int height)
	{
		Polygon polygon = new Polygon();
		polygon.addPoint(x + PATCH_CORNER_CUT, y);
		polygon.addPoint(x + width - PATCH_CORNER_CUT, y);
		polygon.addPoint(x + width, y + PATCH_CORNER_CUT);
		polygon.addPoint(x + width, y + height - PATCH_CORNER_CUT);
		polygon.addPoint(x + width - PATCH_CORNER_CUT, y + height);
		polygon.addPoint(x + PATCH_CORNER_CUT, y + height);
		polygon.addPoint(x, y + height - PATCH_CORNER_CUT);
		polygon.addPoint(x, y + PATCH_CORNER_CUT);
		return polygon;
	}
}
