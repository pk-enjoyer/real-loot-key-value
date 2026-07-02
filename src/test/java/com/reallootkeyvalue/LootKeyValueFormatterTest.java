/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class LootKeyValueFormatterTest
{
	@Test
	public void formatsSmallValuesWithCommas()
	{
		assertEquals("0", LootKeyValueFormatter.formatTabValue(0));
		assertEquals("9,999", LootKeyValueFormatter.formatTabValue(9_999));
		assertEquals("99,999", LootKeyValueFormatter.formatTabValue(99_999));
	}

	@Test
	public void formatsThousandsAsFlooredK()
	{
		assertEquals("100k", LootKeyValueFormatter.formatTabValue(100_000));
		assertEquals("1229k", LootKeyValueFormatter.formatTabValue(1_229_000));
		assertEquals("9999k", LootKeyValueFormatter.formatTabValue(9_999_999));
	}

	@Test
	public void formatsMillionsAndBillionsCompactly()
	{
		assertEquals("10m", LootKeyValueFormatter.formatTabValue(10_000_000));
		assertEquals("12.3m", LootKeyValueFormatter.formatTabValue(12_345_678));
		assertEquals("1b", LootKeyValueFormatter.formatTabValue(1_000_000_000));
		assertEquals("2.1b", LootKeyValueFormatter.formatTabValue(2_147_483_647));
	}

	@Test
	public void formatsChestValueWithFullGpAmount()
	{
		assertEquals("Value in chest: 11,235,717 gp", LootKeyValueFormatter.formatChestValue(11_235_717));
	}

	@Test
	public void formatsGpAmountOnly()
	{
		assertEquals("99,999 gp", LootKeyValueFormatter.formatGpAmount(99_999));
		assertEquals("213,123 gp", LootKeyValueFormatter.formatGpAmount(213_123));
		assertEquals("999,999 gp", LootKeyValueFormatter.formatGpAmount(999_999));
		assertEquals("1,234k", LootKeyValueFormatter.formatGpAmount(1_234_567));
		assertEquals("9,213k", LootKeyValueFormatter.formatGpAmount(9_213_123));
		assertEquals("11.2m", LootKeyValueFormatter.formatGpAmount(11_235_717));
	}
}
