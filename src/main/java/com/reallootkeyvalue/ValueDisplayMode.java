package com.reallootkeyvalue;

public enum ValueDisplayMode
{
	COMPACT_KEY_TAB("Compact key tab"),
	BOTTOM_TEXT("Bottom text"),
	BOTH("Compact key tab and bottom text"),
	KEY_TAB_AND_BOTTOM_TEXT("Key tab and bottom text"),
	TOP_RIGHT_TEXT("Top right text");

	private final String name;

	ValueDisplayMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
