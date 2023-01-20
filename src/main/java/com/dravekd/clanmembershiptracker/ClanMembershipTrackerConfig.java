package com.dravekd.clanmembershiptracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface ClanMembershipTrackerConfig extends Config
{
	@ConfigItem(keyName = "enableUpload", name = "Upload Membership Data", description = "Check to upload your locally tracked membership data on logout to a remote database.")
	default boolean uploadCheckbox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "greeting",
		name = "Welcome Greeting",
		description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello";
	}
}
