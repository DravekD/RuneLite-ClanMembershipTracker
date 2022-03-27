package com.dravekd.clanmembershiptracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClanMembershipTrackerTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanMembershipTrackerPlugin.class);
		RuneLite.main(args);
	}
}