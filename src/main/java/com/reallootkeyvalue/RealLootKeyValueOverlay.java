package com.reallootkeyvalue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOptionClicked;
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
	private static final Color PATCH_BORDER = new Color(38, 32, 25);
	private static final Color PATCH_BORDER_SHADOW = new Color(20, 17, 13);
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final Color MILLION_VALUE_TEXT_COLOR = new Color(13, 196, 102);
	private static final int PATCH_X_OFFSET = 0;
	private static final int PATCH_Y_OFFSET = -1;
	private static final int PATCH_MIN_WIDTH = 35;
	private static final int ICON_Y_OFFSET = 3;
	private static final int TEXT_Y_OFFSET = 31;
	private static final int PATCH_PADDING_X = 2;
	private static final int PATCH_PADDING_BOTTOM = 3;
	private static final int KEY_SLOT_PITCH = 40;
	private static final int PATCH_CORNER_CUT = 4;
	private static final int BOTTOM_TEXT_BASELINE_Y_OFFSET = 208;
	private static final int TOP_RIGHT_TEXT_RIGHT_OFFSET = 278;
	private static final int TOP_RIGHT_TEXT_BASELINE_Y_OFFSET = 11;
	private static final int TOP_RIGHT_TEXT_LINE_GAP = 1;
	private static final int TOP_RIGHT_TEXT_PADDING_X = 3;
	private static final int TOP_RIGHT_TEXT_PADDING_Y = 2;
	private static final String BOTTOM_TEXT_PREFIX = "Value in chest: ";
	private static final Pattern VIEW_TAB_PATTERN = Pattern.compile("(?i)\\bview\\s+tab\\s+(\\d+)\\b");

	private final Client client;
	private final ItemManager itemManager;
	private final LootKeyValueCalculator calculator;
	private final RealLootKeyValueConfig config;
	private Rectangle pendingValueTextBounds;
	private int pendingValueTextKeySlot;
	private String pendingValueText;
	private ValueDisplayMode pendingValueTextMode;
	private Color pendingValueTextColor;
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
			selectedKeySlot = getKeySlot(event.getWidget());
			return;
		}

		final int viewTab = parseViewTab(event.getMenuOption(), event.getMenuTarget());
		if (viewTab >= 0)
		{
			selectedKeySlot = viewTab;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		pendingValueTextBounds = null;
		pendingValueTextKeySlot = -1;
		pendingValueText = null;
		pendingValueTextMode = null;
		pendingValueTextColor = TEXT_COLOR;

		final Shape originalClip = graphics.getClip();
		final Dimension dimension = super.render(graphics);
		graphics.setClip(originalClip);

		if (pendingValueTextBounds != null && pendingValueText != null)
		{
			if (pendingValueTextMode == ValueDisplayMode.TOP_RIGHT_TEXT)
			{
				renderTopRightText(graphics, pendingValueTextBounds, pendingValueTextKeySlot, pendingValueText, pendingValueTextColor);
			}
			else
			{
				renderBottomText(graphics, pendingValueTextBounds, pendingValueTextKeySlot, pendingValueText);
			}
		}

		return dimension;
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
		final ValueDisplayMode displayMode = config.valueDisplayMode();
		if (displayMode == ValueDisplayMode.COMPACT_KEY_TAB || displayMode == ValueDisplayMode.BOTH)
		{
			final String text = LootKeyValueFormatter.formatOverlayValue(value);
			renderReplacementTile(graphics, widgetItem.getCanvasBounds(), text, isTabHovered(keySlot));
		}

		if (displayMode == ValueDisplayMode.KEY_TAB_AND_BOTTOM_TEXT)
		{
			renderKeyImageTile(graphics, widgetItem.getCanvasBounds(), isTabHovered(keySlot));
		}

		if (shouldRenderValueText(displayMode, keySlot))
		{
			if (displayMode == ValueDisplayMode.TOP_RIGHT_TEXT)
			{
				queueValueText(widgetItem, displayMode, keySlot, LootKeyValueFormatter.formatGpAmount(value), value >= 10_000_000L ? MILLION_VALUE_TEXT_COLOR : TEXT_COLOR);
			}
			else
			{
				queueValueText(widgetItem, displayMode, keySlot, LootKeyValueFormatter.formatChestValue(value), TEXT_COLOR);
			}
		}
	}

	private void queueValueText(WidgetItem widgetItem, ValueDisplayMode displayMode, int keySlot, String text, Color textColor)
	{
		pendingValueTextBounds = getNormalizedBounds(widgetItem.getCanvasBounds(), displayMode, keySlot);
		pendingValueTextKeySlot = keySlot;
		pendingValueText = text;
		pendingValueTextMode = displayMode;
		pendingValueTextColor = textColor;
	}

	private Rectangle getNormalizedBounds(Rectangle bounds, ValueDisplayMode displayMode, int keySlot)
	{
		if (displayMode != ValueDisplayMode.TOP_RIGHT_TEXT)
		{
			return bounds;
		}

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

	private boolean shouldRenderValueText(ValueDisplayMode displayMode, int keySlot)
	{
		if (displayMode == ValueDisplayMode.TOP_RIGHT_TEXT)
		{
			return shouldRenderSelectedValueTextForKey(keySlot);
		}

		if (displayMode != ValueDisplayMode.BOTTOM_TEXT &&
			displayMode != ValueDisplayMode.BOTH &&
			displayMode != ValueDisplayMode.KEY_TAB_AND_BOTTOM_TEXT)
		{
			return false;
		}

		return shouldRenderValueTextForKey(keySlot);
	}

	private boolean shouldRenderSelectedValueTextForKey(int keySlot)
	{
		if (keySlot < 0)
		{
			return false;
		}

		if (selectedKeySlot >= 0 && calculator.hasItems(client.getItemContainer(calculator.containerIdForKeySlot(selectedKeySlot))))
		{
			return keySlot == selectedKeySlot;
		}

		return keySlot == getFirstPopulatedKeySlot();
	}

	private boolean shouldRenderValueTextForKey(int keySlot)
	{
		if (keySlot < 0)
		{
			return false;
		}

		if (selectedKeySlot >= 0 && calculator.hasItems(client.getItemContainer(calculator.containerIdForKeySlot(selectedKeySlot))))
		{
			return keySlot == selectedKeySlot;
		}

		return keySlot == getFirstPopulatedKeySlot();
	}

	private int getHoveredKeySlot()
	{
		for (MenuEntry menuEntry : client.getMenuEntries())
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
		return keySlot >= 0 && getHoveredKeySlot() == keySlot;
	}

	private int parseViewTab(String option, String target)
	{
		final String cleanOption = stripTags(option).trim();
		final String cleanTarget = stripTags(target).trim();
		if ("View tab".equalsIgnoreCase(cleanOption))
		{
			return parseIntOrNegativeOne(cleanTarget);
		}

		final String menuText = cleanOption + " " + cleanTarget;
		final Matcher matcher = VIEW_TAB_PATTERN.matcher(menuText);
		if (!matcher.find())
		{
			return -1;
		}

		return parseIntOrNegativeOne(matcher.group(1));
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

	private void renderKeyImageTile(Graphics2D graphics, Rectangle itemBounds, boolean hovered)
	{
		final int patchX = itemBounds.x + PATCH_X_OFFSET;
		final int patchY = itemBounds.y + PATCH_Y_OFFSET;
		final int iconX = patchX + ((PATCH_MIN_WIDTH - LOOT_KEY_IMAGE.getWidth()) / 2);
		final int iconY = itemBounds.y + ICON_Y_OFFSET;
		final int patchHeight = ICON_Y_OFFSET + LOOT_KEY_IMAGE.getHeight() + PATCH_PADDING_BOTTOM;

		graphics.setColor(hovered ? HOVER_BACKGROUND : CHEST_BACKGROUND);
		graphics.fillPolygon(createTabPatch(patchX, patchY, PATCH_MIN_WIDTH, patchHeight));
		graphics.drawImage(LOOT_KEY_IMAGE, iconX, iconY, null);
	}

	private void renderBottomText(Graphics2D graphics, Rectangle bounds, int keySlot, String text)
	{
		graphics.setFont(FontManager.getRunescapeFont());
		final FontMetrics metrics = graphics.getFontMetrics();
		final int firstTabX = bounds.x - (Math.max(0, keySlot) * KEY_SLOT_PITCH);
		final boolean hasPrefix = text.startsWith(BOTTOM_TEXT_PREFIX);
		final int prefixWidth = hasPrefix ? metrics.stringWidth(BOTTOM_TEXT_PREFIX) : 0;
		final String valueText = hasPrefix ? text.substring(BOTTOM_TEXT_PREFIX.length()) : text;
		final int anchorX = firstTabX + ((int) Math.round(KEY_SLOT_PITCH * 2) + 5);
		final int textX = anchorX - prefixWidth - (metrics.stringWidth(valueText) / 2);
		final int textY = bounds.y + BOTTOM_TEXT_BASELINE_Y_OFFSET;

		renderPlainText(graphics, text, textX, textY);
	}

	private void renderTopRightText(Graphics2D graphics, Rectangle bounds, int keySlot, String amountText, Color amountColor)
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
		renderPlainText(graphics, labelText, textX + ((blockWidth - labelMetrics.stringWidth(labelText)) / 2), labelY);
		graphics.setFont(FontManager.getRunescapeFont());
		renderPlainText(graphics, amountText, textX + ((blockWidth - amountMetrics.stringWidth(amountText)) / 2), amountY, amountColor);
	}

	private void renderPlainText(Graphics2D graphics, String text, int x, int y)
	{
		renderPlainText(graphics, text, x, y, TEXT_COLOR);
	}

	private void renderPlainText(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(color);
		graphics.drawString(text, x, y);
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
