package com.reallootkeyvalue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class RealLootKeyValueWidgets
{
	private static final Logger log = LoggerFactory.getLogger(RealLootKeyValueWidgets.class);
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final Color HIGH_VALUE_TEXT_COLOR = new Color(13, 196, 102);
	private static final long HIGH_VALUE_TEXT_THRESHOLD = 10_000_000L;
	private static final String CONFIG_GROUP = "realLootKeyValue";
	private static final int TAB_TEXT_CHILD_OFFSET = 10;
	private static final int TAB_COUNT = 5;

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final LootKeyValueCalculator calculator;
	private final RealLootKeyValueConfig config;
	private final Map<Integer, WidgetTextState> managedTabTextsBySlot = new HashMap<>();
	private final Map<Integer, WidgetTextSnapshot> originalTabTextsBySlot = new HashMap<>();

	@Inject
	RealLootKeyValueWidgets(
		Client client,
		ClientThread clientThread,
		ItemManager itemManager,
		LootKeyValueCalculator calculator,
		RealLootKeyValueConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.calculator = calculator;
		this.config = config;
	}

	void startUp()
	{
		queueRefresh();
	}

	void shutDown()
	{
		clientThread.invoke(() -> restoreTabWidgetTexts(true));
	}

	void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.WILDY_LOOT_CHEST)
		{
			queueRefresh();
		}
	}

	void onConfigChanged(ConfigChanged event)
	{
		if (CONFIG_GROUP.equals(event.getGroup()))
		{
			log.debug("Loot key tab config changed key={} compact={} hideText={} highlightHigh={}",
				event.getKey(),
				config.showCompactKeyTabValue(),
				config.showKeyTabIcon(),
				config.highlightHighValueText());
			queueRefresh();
		}
	}

	void onItemContainerChanged(ItemContainerChanged event)
	{
		if (calculator.keySlotForContainerId(event.getContainerId()) >= 0)
		{
			queueRefresh();
		}
	}

	private void queueRefresh()
	{
		clientThread.invoke(this::refreshWidgetsNow);
	}

	private void refreshWidgetsNow()
	{
		final boolean showCompactValue = config.showCompactKeyTabValue();
		final boolean hideTabText = !showCompactValue && config.showKeyTabIcon();
		log.debug("Loot key tab refresh compact={} hideText={} managedStates={} slotSnapshots={}",
			showCompactValue,
			hideTabText,
			managedTabTextsBySlot.size(),
			originalTabTextsBySlot.size());
		if (!showCompactValue && !hideTabText)
		{
			log.debug("Loot key tab refresh inactive; restoring original text");
			restoreTabWidgetTexts();
			return;
		}

		final Widget tabs = client.getWidget(InterfaceID.WildyLootChest.TABS);
		if (tabs == null || tabs.isHidden())
		{
			log.debug("Loot key tab refresh skipped: tabs widget missing/hidden");
			return;
		}

		final List<Widget> children = getDirectChildren(tabs);
		if (children.size() < TAB_TEXT_CHILD_OFFSET + TAB_COUNT)
		{
			log.debug("Loot key tab refresh skipped: child count {} below required {}", children.size(), TAB_TEXT_CHILD_OFFSET + TAB_COUNT);
			return;
		}

		for (int slot = 0; slot < TAB_COUNT; slot++)
		{
			final Widget textWidget = children.get(TAB_TEXT_CHILD_OFFSET + slot);
			if (hideTabText)
			{
				log.debug("Loot key tab slot {} hide text widget={} index={} bounds={} currentText={}",
					slot,
					textWidget.getId(),
					textWidget.getIndex(),
					textWidget.getBounds(),
					textWidget.getText());
				setTabText(slot, textWidget, "", TEXT_COLOR);
				continue;
			}

			final int containerId = calculator.containerIdForKeySlot(slot);
			if (containerId < 0)
			{
				continue;
			}

			final ItemContainer container = client.getItemContainer(containerId);
			if (!calculator.hasItems(container))
			{
				continue;
			}

			final long value = calculator.calculateGeValue(container, itemManager::getItemPrice);
			final String formattedValue = LootKeyValueFormatter.formatTabValue(value);
			log.debug("Loot key tab slot {} compact value widget={} index={} bounds={} currentText={} value={} formatted={}",
				slot,
				textWidget.getId(),
				textWidget.getIndex(),
				textWidget.getBounds(),
				textWidget.getText(),
				value,
				formattedValue);
			setTabText(slot, textWidget, formattedValue, getValueTextColor(value));
		}
	}

	private List<Widget> getDirectChildren(Widget tabs)
	{
		final List<Widget> children = directChildren(tabs.getChildren());
		if (children.size() >= TAB_TEXT_CHILD_OFFSET + TAB_COUNT)
		{
			return children;
		}

		final List<Widget> dynamicChildren = directChildren(tabs.getDynamicChildren());
		if (dynamicChildren.size() >= TAB_TEXT_CHILD_OFFSET + TAB_COUNT)
		{
			return dynamicChildren;
		}

		final List<Widget> staticChildren = directChildren(tabs.getStaticChildren());
		if (staticChildren.size() >= TAB_TEXT_CHILD_OFFSET + TAB_COUNT)
		{
			return staticChildren;
		}

		return directChildren(tabs.getNestedChildren());
	}

	private List<Widget> directChildren(@Nullable Widget[] children)
	{
		final List<Widget> widgets = new ArrayList<>();
		if (children == null)
		{
			return widgets;
		}

		for (Widget child : children)
		{
			if (child != null)
			{
				widgets.add(child);
			}
		}
		return widgets;
	}

	private void setTabText(int slot, Widget tabTextWidget, String text, Color color)
	{
		final int widgetId = tabTextWidget.getId();
		rememberOriginalText(slot, tabTextWidget);

		final WidgetTextState state = managedTabTextsBySlot.get(slot);
		if (state == null)
		{
			final WidgetTextSnapshot snapshot = originalTabTextsBySlot.get(slot);
			log.debug("Loot key tab slot {} snapshot {} widget={} originalText={} originalColor={}",
				slot,
				"available",
				widgetId,
				snapshot.text,
				snapshot.textColor);
			managedTabTextsBySlot.put(slot, new WidgetTextState(tabTextWidget, snapshot));
		}
		else if (!state.isFor(tabTextWidget))
		{
			log.debug("Loot key tab slot {} reusing snapshot for replaced widget={} originalText={} originalColor={}",
				slot,
				widgetId,
				state.snapshot.text,
				state.snapshot.textColor);
			managedTabTextsBySlot.put(slot, state.forWidget(tabTextWidget));
		}

		log.debug("Loot key tab slot {} set widget={} text={} color={} previousText={}",
			slot,
			widgetId,
			text,
			color.getRGB() & 0x00ffffff,
			tabTextWidget.getText());
		managedTabTextsBySlot.put(slot, new WidgetTextState(tabTextWidget, originalTabTextsBySlot.get(slot), text));
		tabTextWidget.setText(text);
		tabTextWidget.setTextColor(color.getRGB() & 0x00ffffff);
		tabTextWidget.revalidate();
	}

	private void rememberOriginalText(int slot, Widget tabTextWidget)
	{
		final WidgetTextSnapshot existingSnapshot = originalTabTextsBySlot.get(slot);
		if (existingSnapshot == null)
		{
			final WidgetTextSnapshot snapshot = new WidgetTextSnapshot(tabTextWidget);
			originalTabTextsBySlot.put(slot, snapshot);
			log.debug("Loot key tab slot {} snapshot created widget={} text={} color={}",
				slot,
				tabTextWidget.getId(),
				snapshot.text,
				snapshot.textColor);
			return;
		}

		if (existingSnapshot.text.isEmpty() && !tabTextWidget.getText().isEmpty() && !isPluginWrittenText(slot, tabTextWidget.getText()))
		{
			final WidgetTextSnapshot snapshot = new WidgetTextSnapshot(tabTextWidget);
			originalTabTextsBySlot.put(slot, snapshot);
			final WidgetTextState state = managedTabTextsBySlot.get(slot);
			if (state != null)
			{
				managedTabTextsBySlot.put(slot, new WidgetTextState(state.widget, snapshot));
			}
			log.debug("Loot key tab slot {} snapshot upgraded widget={} oldText={} newText={} newColor={}",
				slot,
				tabTextWidget.getId(),
				existingSnapshot.text,
				snapshot.text,
				snapshot.textColor);
		}
	}

	private void restoreTabWidgetTexts()
	{
		restoreTabWidgetTexts(false);
	}

	private void restoreTabWidgetTexts(boolean clearOriginals)
	{
		log.debug("Loot key tab restore start clearOriginals={} managedStates={} slotSnapshots={}",
			clearOriginals,
			managedTabTextsBySlot.size(),
			originalTabTextsBySlot.size());
		boolean restoredLiveWidgets = false;
		final Widget tabs = client.getWidget(InterfaceID.WildyLootChest.TABS);
		if (tabs != null && !tabs.isHidden())
		{
			final List<Widget> children = getDirectChildren(tabs);
			if (children.size() >= TAB_TEXT_CHILD_OFFSET + TAB_COUNT)
			{
				for (int slot = 0; slot < TAB_COUNT; slot++)
				{
					final Widget textWidget = children.get(TAB_TEXT_CHILD_OFFSET + slot);
					final WidgetTextState state = managedTabTextsBySlot.get(slot);
					if (state != null)
					{
						log.debug("Loot key tab restore slot {} via managed state widget={} index={} bounds={} currentText={} originalText={}",
							slot,
							textWidget.getId(),
							textWidget.getIndex(),
							textWidget.getBounds(),
							textWidget.getText(),
							state.snapshot.text);
						state.restore(textWidget);
						restoredLiveWidgets = true;
						continue;
					}

					final WidgetTextSnapshot snapshot = originalTabTextsBySlot.get(slot);
					if (snapshot != null)
					{
						log.debug("Loot key tab restore slot {} via slot snapshot widget={} index={} bounds={} currentText={} originalText={}",
							slot,
							textWidget.getId(),
							textWidget.getIndex(),
							textWidget.getBounds(),
							textWidget.getText(),
							snapshot.text);
						snapshot.restore(textWidget);
						restoredLiveWidgets = true;
					}
				}
			}
			else
			{
				log.debug("Loot key tab restore live widgets skipped: child count {} below required {}", children.size(), TAB_TEXT_CHILD_OFFSET + TAB_COUNT);
			}
		}
		else
		{
			log.debug("Loot key tab restore live widgets skipped: tabs widget missing/hidden");
		}

		for (WidgetTextState state : managedTabTextsBySlot.values())
		{
			log.debug("Loot key tab restore fallback widget={} originalText={}", state.widget.getId(), state.snapshot.text);
			state.restore();
		}
		if (restoredLiveWidgets || clearOriginals)
		{
			log.debug("Loot key tab restore clearing managed states restoredLiveWidgets={} clearOriginals={}", restoredLiveWidgets, clearOriginals);
			managedTabTextsBySlot.clear();
		}
		if (clearOriginals)
		{
			log.debug("Loot key tab restore clearing slot snapshots");
			originalTabTextsBySlot.clear();
		}
	}

	private Color getValueTextColor(long value)
	{
		return config.highlightHighValueText() && value >= HIGH_VALUE_TEXT_THRESHOLD ? HIGH_VALUE_TEXT_COLOR : TEXT_COLOR;
	}

	private boolean isPluginWrittenText(int slot, String text)
	{
		final WidgetTextState state = managedTabTextsBySlot.get(slot);
		return state != null && state.writtenText.equals(text);
	}

	private static final class WidgetTextState
	{
		private final Widget widget;
		private final WidgetTextSnapshot snapshot;
		private final String writtenText;

		private WidgetTextState(Widget widget, WidgetTextSnapshot snapshot)
		{
			this(widget, snapshot, widget.getText());
		}

		private WidgetTextState(Widget widget, WidgetTextSnapshot snapshot, String writtenText)
		{
			this.widget = widget;
			this.snapshot = snapshot;
			this.writtenText = writtenText;
		}

		private boolean isFor(Widget other)
		{
			return widget == other;
		}

		private WidgetTextState forWidget(Widget other)
		{
			return new WidgetTextState(other, snapshot, writtenText);
		}

		private void restore()
		{
			if (widget == null)
			{
				return;
			}

			snapshot.restore(widget);
		}

		private void restore(Widget target)
		{
			snapshot.restore(target);
		}
	}

	private static final class WidgetTextSnapshot
	{
		private final String text;
		private final int textColor;

		private WidgetTextSnapshot(Widget widget)
		{
			text = widget.getText();
			textColor = widget.getTextColor();
		}

		private void restore(Widget target)
		{
			target.setText(text);
			target.setTextColor(textColor);
			target.revalidate();
		}
	}
}
