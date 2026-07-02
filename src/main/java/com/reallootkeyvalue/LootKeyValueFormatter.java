/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

final class LootKeyValueFormatter
{
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.ENGLISH);
	private static final DecimalFormat ONE_DECIMAL_FORMAT = new DecimalFormat("#,###.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

	private LootKeyValueFormatter()
	{
	}

	static String formatTabValue(long value)
	{
		if (value < 100_000L)
		{
			return INTEGER_FORMAT.format(value);
		}

		if (value < 10_000_000L)
		{
			return (value / 1_000L) + "k";
		}

		if (value < 1_000_000_000L)
		{
			return formatOneDecimal(value, 1_000_000L) + "m";
		}

		return formatOneDecimal(value, 1_000_000_000L) + "b";
	}

	static String formatChestValue(long value)
	{
		return "Value in chest: " + INTEGER_FORMAT.format(value) + " gp";
	}

	static String formatGpAmount(long value)
	{
		if (value < 1_000_000L)
		{
			return INTEGER_FORMAT.format(value) + " gp";
		}

		if (value < 10_000_000L)
		{
			return INTEGER_FORMAT.format(value / 1_000L) + "k";
		}

		if (value < 1_000_000_000L)
		{
			return formatOneDecimal(value, 1_000_000L) + "m";
		}

		return formatOneDecimal(value, 1_000_000_000L) + "b";
	}

	private static String formatOneDecimal(long value, long divisor)
	{
		final long floored = (value * 10L) / divisor;
		return ONE_DECIMAL_FORMAT.format(floored / 10.0D);
	}

}
