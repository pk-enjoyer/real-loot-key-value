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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
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
	private static final BufferedImage LOOT_KEY_IMAGE_WITH_INNER_SHADOW = createInnerShadowImage(LOOT_KEY_IMAGE);
	private static final BufferedImage LOOT_KEY_IMAGE_CAST_SHADOW = createCastShadowImage(LOOT_KEY_IMAGE);
	private static final Color CHEST_BACKGROUND = new Color(64, 55, 43);
	private static final Color KEY_TAB_GRADIENT_TOP = new Color(0x3a3329);
	private static final Color KEY_TAB_GRADIENT_BOTTOM = new Color(0x453d32);
	private static final Color HOVER_BACKGROUND = new Color(0x453d32);
	private static final Color PATCH_BORDER = new Color(38, 32, 25);
	private static final Color PATCH_BORDER_SHADOW = new Color(20, 17, 13);
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final Color HIGH_VALUE_TEXT_COLOR = new Color(13, 196, 102);
	private static final long HIGH_VALUE_TEXT_THRESHOLD = 10_000_000L;
	private static final int PATCH_X_OFFSET = 0;
	private static final int PATCH_Y_OFFSET = -1;
	private static final int PATCH_MIN_WIDTH = 35;
	private static final int ICON_X_OFFSET = -8;
	private static final int ICON_Y_OFFSET = 1;
	private static final int ICON_CAST_SHADOW_X_OFFSET = 1;
	private static final int ICON_CAST_SHADOW_Y_OFFSET = 0;
	private static final int TEXT_Y_OFFSET = 33;
	private static final int PATCH_PADDING_X = 2;
	private static final int PATCH_PADDING_BOTTOM = 0;
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
	private Rectangle pendingBottomTextBounds;
	private int pendingBottomTextKeySlot;
	private String pendingBottomText;
	private Rectangle pendingTopRightTextBounds;
	private String pendingTopRightText;
	private Color pendingTopRightTextColor;
	private final List<KeyImageTile> pendingKeyImageTiles = new ArrayList<>();
	private int selectedKeySlot = -1;
	private boolean refreshSelectedKeySlotFromChest;

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

	void onMenuOpened(MenuOpened event)
	{
		final int menuKeySlot = getKeySlot(event.getMenuEntries());
		if (menuKeySlot >= 0)
		{
			selectedKeySlot = menuKeySlot;
		}
	}

	void onItemContainerChanged(ItemContainerChanged event)
	{
		if (calculator.keySlotForContainerId(event.getContainerId()) < 0)
		{
			return;
		}

		refreshSelectedKeySlotFromChest = true;
		if (!hasSelectedKeySlotItems())
		{
			selectedKeySlot = getFirstPopulatedKeySlot();
		}
	}

	void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.WILDY_LOOT_CHEST)
		{
			refreshSelectedKeySlotFromChest = true;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		refreshSelectedKeySlotFromChest();

		pendingBottomTextBounds = null;
		pendingBottomTextKeySlot = -1;
		pendingBottomText = null;
		pendingTopRightTextBounds = null;
		pendingTopRightText = null;
		pendingTopRightTextColor = TEXT_COLOR;
		pendingKeyImageTiles.clear();

		final Shape originalClip = graphics.getClip();
		final Dimension dimension = super.render(graphics);
		graphics.setClip(originalClip);

		for (KeyImageTile keyImageTile : pendingKeyImageTiles)
		{
			renderKeyImageTile(graphics, keyImageTile.bounds, isTabHovered(keyImageTile.keySlot), isTabActive(keyImageTile.keySlot));
		}

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
		if (config.showCompactKeyTabValue())
		{
			final String text = LootKeyValueFormatter.formatOverlayValue(value);
			renderReplacementTile(graphics, widgetItem.getCanvasBounds(), text, valueTextColor, isTabHovered(keySlot), isTabActive(keySlot));
		}
		else if (config.showKeyTabIcon())
		{
			queueKeyImageTile(widgetItem);
		}

		if (config.showBottomText() && shouldRenderValueTextForKey(keySlot))
		{
			queueBottomText(widgetItem, keySlot, LootKeyValueFormatter.formatChestValue(value));
		}

		if (config.showTopRightText() && shouldRenderValueTextForKey(keySlot))
		{
			queueTopRightText(widgetItem, keySlot, LootKeyValueFormatter.formatGpAmount(value), valueTextColor);
		}
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

	private void queueKeyImageTile(WidgetItem widgetItem)
	{
		pendingKeyImageTiles.add(new KeyImageTile(widgetItem.getCanvasBounds(), getKeySlot(widgetItem)));
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

	private int getHoveredKeySlot()
	{
		return getKeySlot(client.getMenuEntries());
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

	private void refreshSelectedKeySlotFromChest()
	{
		if (!refreshSelectedKeySlotFromChest)
		{
			return;
		}

		final int visibleKeySlot = getVisibleChestKeySlot();
		if (visibleKeySlot >= 0)
		{
			selectedKeySlot = visibleKeySlot;
			refreshSelectedKeySlotFromChest = false;
		}
	}

	private int getVisibleChestKeySlot()
	{
		final int selectedTabKeySlot = getSelectedTabKeySlot();
		if (selectedTabKeySlot >= 0)
		{
			return selectedTabKeySlot;
		}

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

		for (int slot = 0; ; slot++)
		{
			final int containerId = calculator.containerIdForKeySlot(slot);
			if (containerId < 0)
			{
				return -1;
			}

			if (visibleItems.equals(getContainerItems(client.getItemContainer(containerId))))
			{
				return slot;
			}
		}
	}

	private int getSelectedTabKeySlot()
	{
		final Widget tabsWidget = client.getWidget(InterfaceID.WildyLootChest.TABS);
		if (tabsWidget == null || tabsWidget.isHidden())
		{
			return -1;
		}

		final Widget[] tabWidgets = getWidgetChildren(tabsWidget);
		if (tabWidgets == null)
		{
			return -1;
		}

		for (int slot = 0; slot < Math.min(5, tabWidgets.length); slot++)
		{
			final Widget tabWidget = tabWidgets[slot];
			if (tabWidget == null || tabWidget.getOnOpListener() != null)
			{
				continue;
			}

			if (calculator.hasItems(client.getItemContainer(calculator.containerIdForKeySlot(slot))))
			{
				return slot;
			}
		}

		return -1;
	}

	private Widget[] getWidgetChildren(Widget widget)
	{
		Widget[] children = widget.getDynamicChildren();
		if (children != null && children.length > 0)
		{
			return children;
		}

		children = widget.getStaticChildren();
		if (children != null && children.length > 0)
		{
			return children;
		}

		return widget.getChildren();
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

	private boolean isTabActive(int keySlot)
	{
		return keySlot >= 0 && selectedKeySlot == keySlot;
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

	private void renderReplacementTile(Graphics2D graphics, Rectangle itemBounds, String text, Color textColor, boolean hovered, boolean active)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());

		final FontMetrics metrics = graphics.getFontMetrics();
		final int textWidth = metrics.stringWidth(text);
		final int patchWidth = Math.max(PATCH_MIN_WIDTH, textWidth + (PATCH_PADDING_X * 2));
		final int patchX = itemBounds.x + PATCH_X_OFFSET;
		final int patchY = itemBounds.y + PATCH_Y_OFFSET;
		final int iconX = patchX + ((patchWidth - LOOT_KEY_IMAGE_WITH_INNER_SHADOW.getWidth()) / 2);
		final int iconY = itemBounds.y + ICON_Y_OFFSET;
		final int textX = patchX + ((patchWidth - textWidth) / 2);
		final int textBaselineY = itemBounds.y + TEXT_Y_OFFSET;
		final int patchHeight = (textBaselineY - patchY) + metrics.getDescent() + PATCH_PADDING_BOTTOM;

		fillTabPatch(graphics, patchX, patchY, patchWidth, patchHeight, hovered, active);
		graphics.drawImage(LOOT_KEY_IMAGE_CAST_SHADOW, iconX + ICON_CAST_SHADOW_X_OFFSET, iconY + ICON_CAST_SHADOW_Y_OFFSET, null);
		graphics.drawImage(LOOT_KEY_IMAGE_WITH_INNER_SHADOW, iconX, iconY, null);

		final TextComponent textComponent = new TextComponent();
		textComponent.setPosition(new Point(textX, textBaselineY));
		textComponent.setText(text);
		textComponent.setColor(textColor);
		textComponent.render(graphics);
	}

	private void renderKeyImageTile(Graphics2D graphics, Rectangle itemBounds, boolean hovered, boolean active)
	{
		graphics.setFont(FontManager.getRunescapeSmallFont());

		final FontMetrics metrics = graphics.getFontMetrics();
		final int patchX = itemBounds.x + PATCH_X_OFFSET;
		final int patchY = itemBounds.y + PATCH_Y_OFFSET;
		final int textBaselineY = itemBounds.y + TEXT_Y_OFFSET;
		final int patchHeight = (textBaselineY - patchY) + metrics.getDescent() + PATCH_PADDING_BOTTOM;
		final int iconX = patchX + ((PATCH_MIN_WIDTH - LOOT_KEY_IMAGE_WITH_INNER_SHADOW.getWidth()) / 2) + ICON_X_OFFSET;
		final int iconY = itemBounds.y + ICON_Y_OFFSET;

		fillTabPatch(graphics, patchX, patchY, PATCH_MIN_WIDTH, patchHeight, hovered, active);
		graphics.drawImage(LOOT_KEY_IMAGE_CAST_SHADOW, iconX + ICON_CAST_SHADOW_X_OFFSET, iconY + ICON_CAST_SHADOW_Y_OFFSET, null);
		graphics.drawImage(LOOT_KEY_IMAGE_WITH_INNER_SHADOW, iconX, iconY, null);
	}

	private void renderBottomText(Graphics2D graphics, Rectangle bounds, int keySlot, String text)
	{
		graphics.setFont(FontManager.getRunescapeFont());
		final FontMetrics metrics = graphics.getFontMetrics();
		final int firstTabX = bounds.x - (Math.max(0, keySlot) * KEY_SLOT_PITCH);
		final String valueText = text.startsWith(BOTTOM_TEXT_PREFIX) ? text.substring(BOTTOM_TEXT_PREFIX.length()) : text;
		final int valueTextWidth = metrics.stringWidth(valueText);
		final int gpTextWidth = metrics.stringWidth(" gp") - 1;
		final int textX = firstTabX + ((int) Math.round(KEY_SLOT_PITCH * 2) + gpTextWidth) - ((valueTextWidth/2));
		final int textY = bounds.y + BOTTOM_TEXT_BASELINE_Y_OFFSET;

		renderPlainText(graphics, text, textX, textY);
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

	private void fillTabPatch(Graphics2D graphics, int x, int y, int width, int height, boolean hovered, boolean active)
	{
		final Polygon patch = createTabPatch(x, y, width, height);
		if (hovered || active)
		{
			graphics.setColor(HOVER_BACKGROUND);
			graphics.fillPolygon(patch);
			return;
		}

		final Shape originalClip = graphics.getClip();
		graphics.clip(patch);

		final int fadeStartY = y + ((height * 3) / 5);
		final int fadeHeight = Math.max(1, (y + height) - fadeStartY);
		graphics.setColor(KEY_TAB_GRADIENT_TOP);
		graphics.fillRect(x, y, width, fadeStartY - y);

		for (int step = 0; step < 5; step++)
		{
			final int bandY = fadeStartY + ((fadeHeight * step) / 5);
			final int nextBandY = fadeStartY + ((fadeHeight * (step + 1)) / 5);
			graphics.setColor(lerp(KEY_TAB_GRADIENT_TOP, KEY_TAB_GRADIENT_BOTTOM, step + 1, 5));
			graphics.fillRect(x, bandY, width, Math.max(1, nextBandY - bandY));
		}

		graphics.setClip(originalClip);
	}

	private Color lerp(Color from, Color to, int step, int steps)
	{
		final int red = from.getRed() + (((to.getRed() - from.getRed()) * step) / steps);
		final int green = from.getGreen() + (((to.getGreen() - from.getGreen()) * step) / steps);
		final int blue = from.getBlue() + (((to.getBlue() - from.getBlue()) * step) / steps);
		return new Color(red, green, blue);
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

	private static BufferedImage createInnerShadowImage(BufferedImage source)
	{
		final BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				final int argb = source.getRGB(x, y);
				final int alpha = (argb >>> 24) & 0xff;
				if (alpha == 0)
				{
					image.setRGB(x, y, argb);
					continue;
				}

				final int shadow = getInnerShadowStrength(source, x, y);
				image.setRGB(x, y, darken(argb, shadow));
			}
		}

		return image;
	}

	private static BufferedImage createCastShadowImage(BufferedImage source)
	{
		final BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				final int alpha = (source.getRGB(x, y) >>> 24) & 0xff;
				if (alpha == 0)
				{
					continue;
				}

				image.setRGB(x, y, Math.min(95, alpha / 2) << 24);
			}
		}

		return image;
	}

	private static int getInnerShadowStrength(BufferedImage source, int x, int y)
	{
		int strength = 0;
		for (int dy = -4; dy <= 4; dy++)
		{
			for (int dx = -4; dx <= 4; dx++)
			{
				if (dx == 0 && dy == 0 || Math.abs(dx) + Math.abs(dy) > 6)
				{
					continue;
				}

				if (!isTransparent(source, x + dx, y + dy))
				{
					continue;
				}

				final int distance = Math.max(Math.abs(dx), Math.abs(dy));
				final int directionalWeight = dx <= 0 && dy <= 0 ? 28 : 16;
				strength = Math.max(strength, directionalWeight - (distance * 3));
			}
		}

		return strength;
	}

	private static boolean isTransparent(BufferedImage source, int x, int y)
	{
		if (x < 0 || y < 0 || x >= source.getWidth() || y >= source.getHeight())
		{
			return true;
		}

		return ((source.getRGB(x, y) >>> 24) & 0xff) < 24;
	}

	private static int darken(int argb, int amount)
	{
		if (amount <= 0)
		{
			return argb;
		}

		final int alpha = (argb >>> 24) & 0xff;
		final int red = Math.max(0, ((argb >>> 16) & 0xff) - amount);
		final int green = Math.max(0, ((argb >>> 8) & 0xff) - amount);
		final int blue = Math.max(0, (argb & 0xff) - amount);
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	private static final class KeyImageTile
	{
		private final Rectangle bounds;
		private final int keySlot;

		private KeyImageTile(Rectangle bounds, int keySlot)
		{
			this.bounds = bounds;
			this.keySlot = keySlot;
		}
	}
}
