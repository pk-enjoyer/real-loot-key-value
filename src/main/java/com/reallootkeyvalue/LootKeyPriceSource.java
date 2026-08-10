/*
 * Copyright (c) 2025, pk-enjoyer
 * All rights reserved.
 *
 * This source code is licensed under the BSD 2-Clause license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reallootkeyvalue;

public enum LootKeyPriceSource
{
	RUNELITE("RuneLite actively traded"),
	JAGEX("Jagex guide price");

	private final String displayName;

	LootKeyPriceSource(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
