package com.dravekd.clanmembershiptracker;


import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

@Slf4j
@PluginDescriptor(
		name = "Clan Membership Tracker"
)
public class ClanMembershipTrackerPlugin extends Plugin
{
	public enum ClanType {
		USER, GUEST
	}

	private static final int NUMTICKSBEFOREUPDATING = 100;
	private static final File CLAN_DIR = new File(RUNELITE_DIR, "clans");

	@Nullable
	private ClanTracker userClanData;
	@Nullable
	private File userClanFile;
	private File userClanExportFile;

	@Nullable
	private ClanTracker guestClanData;
	@Nullable
	private File guestClanFile;
	private File guestClanExportFile;

	private GameState lastGameState;
	private int ticksSinceLastUpdate;
	private Boolean currentlyRunningUpdate;
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Inject
	private Client client;

	@Inject
	private ClanMembershipTrackerConfig config;

	@Inject
	private ClanTrackerDataManager clanTrackerDataManager;

	@Override
	protected void startUp() throws Exception
	{
		CLAN_DIR.mkdirs();
		ticksSinceLastUpdate = 0;
		currentlyRunningUpdate = false;
		log.info("Clan Membership Tracker started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Clan Membership Tracker stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (lastGameState == GameState.LOGGED_IN && gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			writeDataFile();
			List<ClanTracker> clans = new ArrayList<ClanTracker>();
			if (userClanData != null)
			{
				clans.add(userClanData);
			}
			if (guestClanData != null)
			{
				clans.add(guestClanData);
			}
			if (clans.size() > 0)
			{
				clanTrackerDataManager.updateClanMembership(clans);
			}
			//exportDataFile();
		}

		lastGameState = gameStateChanged.getGameState();
	}

	@Provides
	ClanMembershipTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanMembershipTrackerConfig.class);
	}

	private void initializeFileAndData(ClanType clanType)
	{
		if (clanType == ClanType.USER)
		{
			if (client.getClanChannel() == null)
			{
				userClanFile = null;
				userClanData = null;
			}
			else
			{
				String fileName = client.getClanChannel().getName();
				userClanFile = new File(CLAN_DIR, fileName + ".clan");
				userClanExportFile = new File(CLAN_DIR, fileName + ".csv");
				if (!userClanFile.exists())
				{
					userClanData = new ClanTracker(fileName);
				}
				else
				{
					userClanData = new ClanTracker(fileName);
					readDataFile(ClanType.USER);
				}
			}
		}
		else if (clanType == ClanType.GUEST)
		{
			if (client.getGuestClanChannel() == null)
			{
				guestClanFile = null;
				guestClanData = null;
			}
			else
			{
				String fileName = client.getGuestClanChannel().getName();
				guestClanFile = new File(CLAN_DIR, fileName + ".clan");
				guestClanExportFile = new File(CLAN_DIR, fileName + ".csv");
				if (!guestClanFile.exists())
				{
					guestClanData = new ClanTracker(fileName);
				}
				else
				{
					guestClanData = new ClanTracker(fileName);
					readDataFile(ClanType.GUEST);
				}
			}
		}
	}

	private void updateMembershipDatas()
	{
		if (client.getClanChannel() != null)
		{
			if (!client.getClanChannel().getName().equals(userClanData.name))
				initializeFileAndData(ClanType.USER);

			LocalDateTime timeUpdated = LocalDateTime.now();
			updateClanMembershipList(timeUpdated, ClanType.USER);
			updateClanOnlineMembership(timeUpdated, ClanType.USER);
		}
		if (client.getGuestClanChannel() != null)
		{
			if (!client.getGuestClanChannel().getName().equals(guestClanData.name))
				initializeFileAndData(ClanType.GUEST);

			LocalDateTime timeUpdated = LocalDateTime.now();
			updateClanMembershipList(timeUpdated, ClanType.GUEST);
			updateClanOnlineMembership(timeUpdated, ClanType.GUEST);
		}
	}

	private void updateClanMembershipList(LocalDateTime timeUpdated, ClanType clanType)
	{
		List<ClanMember> allMembers;
		ClanTracker clanData;
		if (clanType == ClanType.USER)
		{
			allMembers = client.getClanSettings().getMembers();
			clanData = userClanData;
		}
		else
		{
			allMembers = client.getGuestClanSettings().getMembers();
			clanData = guestClanData;
		}

		allMembers.forEach((clanMember) ->
		{
			if (clanData.clanMembers == null)
			{
				ClanMemberTracker newMember = new ClanMemberTracker(clanMember.getName(), clanMember.getRank(), timeUpdated);
				clanData.clanMembers.add(newMember);
			}
			else
			{
				ClanMemberTracker tracker = clanData.clanMembers.stream().filter(c -> clanMember.getName().equals(c.displayName)).findFirst().orElse(null);
				if (tracker == null)
				{
					ClanMemberTracker newMember = new ClanMemberTracker(clanMember.getName(), clanMember.getRank(), timeUpdated);
					clanData.clanMembers.add(newMember);
				}
			}
		});

		List<ClanMemberTracker> clanMembersToRemove = new ArrayList<ClanMemberTracker>();
		clanData.clanMembers.forEach((clanMemberTracker) ->
		{
			ClanMember member = allMembers.stream().filter(m -> clanMemberTracker.displayName.equals(m.getName())).findFirst().orElse(null);
			if (member == null)
			{
				clanMembersToRemove.add(clanMemberTracker);
			}
		});
		clanMembersToRemove.forEach((clanMember) ->
		{
			clanData.clanMembers.remove(clanMember);
		});
	}

	private void updateClanOnlineMembership(LocalDateTime timeUpdated, ClanType clanType)
	{
		List<ClanChannelMember> onlineMembers;
		ClanTracker clanData;
		if (clanType == ClanType.USER)
		{
			onlineMembers = client.getClanChannel().getMembers();
			clanData = userClanData;
		}
		else
		{
			onlineMembers = client.getGuestClanChannel().getMembers();
			clanData = guestClanData;
		}

		onlineMembers.forEach((clanMember) ->
		{
			ClanMemberTracker tracker = clanData.clanMembers.stream().filter(c -> clanMember.getName().equals(c.displayName)).findFirst().orElse(null);
			if (tracker != null)
			{
				tracker.lastLogDate = timeUpdated;
			}
		});
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() == ChatMessageType.CLAN_CHAT)
		{
			LocalDateTime timeUpdated = LocalDateTime.now();
			String chatterName = chatMessage.getName().substring(chatMessage.getName().indexOf('>') + 1);
			ClanMemberTracker tracker = userClanData.clanMembers.stream().filter(c -> chatterName.equals(c.displayName)).findFirst().orElse(null);
			if (tracker == null)
			{
				ClanMemberTracker newMember = new ClanMemberTracker(chatterName, timeUpdated);
				newMember.lastChatDate = timeUpdated;
			}
			else
			{
				tracker.lastChatDate = timeUpdated;
			}
		}
		else if (chatMessage.getType() == ChatMessageType.CLAN_GUEST_CHAT)
		{
			LocalDateTime timeUpdated = LocalDateTime.now();
			String chatterName = chatMessage.getName().substring(chatMessage.getName().indexOf('>') + 1);
			ClanMemberTracker tracker = guestClanData.clanMembers.stream().filter(c -> chatterName.equals(c.displayName)).findFirst().orElse(null);
			if (tracker == null)
			{
				ClanMemberTracker newMember = new ClanMemberTracker(chatterName, timeUpdated);
				newMember.lastChatDate = timeUpdated;
			}
			else
			{
				tracker.lastChatDate = timeUpdated;
			}
		}
	}

	private void readDataFile(ClanType clanType)
	{
		File workingFile;
		ClanTracker clanData;
		if (clanType == ClanType.USER)
		{
			workingFile = userClanFile;
			clanData = userClanData;
		}
		else
		{
			workingFile = guestClanFile;
			clanData = guestClanData;
		}

		try (FileInputStream in = new FileInputStream(workingFile);
			 ObjectInputStream inObj = new ObjectInputStream(in))
		{
			clanData = (ClanTracker)inObj.readObject();
		}
		catch (Exception ex)
		{
			log.error(ex.toString());
		}
	}

	private void writeDataFile()
	{
		if (userClanData != null && userClanFile != null)
		{
			try (FileOutputStream out = new FileOutputStream(userClanFile);
				 ObjectOutputStream objOut = new ObjectOutputStream(out))
			{
				objOut.writeObject(userClanData);
				objOut.flush();
			}
			catch (Exception ex)
			{
				log.error(ex.toString());
			}
		}
		if (guestClanData != null && guestClanFile != null)
		{
			try (FileOutputStream out = new FileOutputStream(guestClanFile);
				 ObjectOutputStream objOut = new ObjectOutputStream(out))
			{
				objOut.writeObject(guestClanData);
				objOut.flush();
			}
			catch (Exception ex)
			{
				log.error(ex.toString());
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (currentlyRunningUpdate)
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			//ticksSinceLastUpdate += 1;
			return;
		}
		if (ticksSinceLastUpdate < NUMTICKSBEFOREUPDATING)
		{
			ticksSinceLastUpdate += 1;
			return;
		}

		currentlyRunningUpdate = true;
		try
		{
			updateMembershipDatas();
		}
		catch (Exception ex)
		{
			log.error(ex.toString());
		}
		ticksSinceLastUpdate = 0;
		currentlyRunningUpdate = false;
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (event.getClanChannel() != null && event.getClanId() == ClanID.CLAN && !event.isGuest())
		{
			initializeFileAndData(ClanType.USER);
		}
		else if (event.getClanChannel() != null && event.isGuest())
		{
			initializeFileAndData(ClanType.GUEST);
		}
	}

	private void exportDataFile()
	{
		if (userClanExportFile != null && userClanData != null)
		{
			try (PrintWriter writer = new PrintWriter(userClanExportFile, "UTF-8");
			)
			{
				writer.println("Name,LastLoggedIn,LastChattedInClan");
				userClanData.clanMembers.forEach((clanMember) ->
				{
					String line = clanMember.displayName + ",";
					if (clanMember.lastLogDate != null)
						line += clanMember.lastLogDate.format(dateTimeFormatter) + ",";
					else
						line += "null,";
					if (clanMember.lastChatDate != null)
						line += clanMember.lastChatDate.format(dateTimeFormatter);
					else
						line += "null";

					writer.println(line);
				});
			}
			catch (Exception ex)
			{
				log.error(ex.toString());
			}
		}

		if (guestClanExportFile != null && guestClanData != null)
		{
			try (PrintWriter writer = new PrintWriter(guestClanExportFile, "UTF-8");
			)
			{
				writer.println("Name,LastLoggedIn,LastChattedInClan");
				guestClanData.clanMembers.forEach((clanMember) ->
				{
					String line = clanMember.displayName + ",";
					if (clanMember.lastLogDate != null)
						line += clanMember.lastLogDate.format(dateTimeFormatter) + ",";
					else
						line += "null,";
					if (clanMember.lastChatDate != null)
						line += clanMember.lastChatDate.format(dateTimeFormatter);
					else
						line += "null";

					writer.println(line);
				});
			}
			catch (Exception ex)
			{
				log.error(ex.toString());
			}
		}
	}
}
