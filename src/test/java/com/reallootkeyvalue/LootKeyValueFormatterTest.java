package com.reallootkeyvalue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LootKeyValueFormatterTest
{
	@Test
	public void formatsSmallValuesWithCommas()
	{
		assertEquals("0", LootKeyValueFormatter.formatOverlayValue(0));
		assertEquals("9,999", LootKeyValueFormatter.formatOverlayValue(9_999));
		assertEquals("99,999", LootKeyValueFormatter.formatOverlayValue(99_999));
	}

	@Test
	public void formatsThousandsAsFlooredK()
	{
		assertEquals("100K", LootKeyValueFormatter.formatOverlayValue(100_000));
		assertEquals("1229K", LootKeyValueFormatter.formatOverlayValue(1_229_000));
		assertEquals("9999K", LootKeyValueFormatter.formatOverlayValue(9_999_999));
	}

	@Test
	public void formatsMillionsAndBillionsCompactly()
	{
		assertEquals("10M", LootKeyValueFormatter.formatOverlayValue(10_000_000));
		assertEquals("12.3M", LootKeyValueFormatter.formatOverlayValue(12_345_678));
		assertEquals("1B", LootKeyValueFormatter.formatOverlayValue(1_000_000_000));
		assertEquals("2.1B", LootKeyValueFormatter.formatOverlayValue(2_147_483_647));
	}
}
