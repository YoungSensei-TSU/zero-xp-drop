package com.sensei.zeroxpdrop;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SenseiZeroXpDropPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SenseiZeroXpDropPlugin.class);
		RuneLite.main(args);
	}
}
