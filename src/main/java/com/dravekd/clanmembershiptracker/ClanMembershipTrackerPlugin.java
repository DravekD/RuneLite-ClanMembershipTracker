package com.dravekd.clanmembershiptracker;


import com.google.inject.Provides;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.clan.ClanChannel;
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
	private static final int NUMTICKSBEFOREUPDATING = 50;
	private static final File CLAN_DIR = new File(RUNELITE_DIR, "clans");
	@Nullable
	private ClanTracker workingData;
	@Nullable
	private File workingFile;
	private File exportFile;
	private int ticksSinceLastUpdate;
	private Boolean currentlyRunningUpdate;
	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Inject
	private Client client;

	@Inject
	private ClanMembershipTrackerConfig config;

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
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			writeDataFile();
			exportDataFile();
		}
	}

	@Provides
	ClanMembershipTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanMembershipTrackerConfig.class);
	}

	private void initializeFileAndData()
	{
		if (client.getClanChannel() == null)
		{
			workingFile = null;
			workingData = null;
		}
		else
		{
			String fileName = client.getClanChannel().getName();
			workingFile = new File(CLAN_DIR, fileName + ".txt");
			exportFile = new File(CLAN_DIR, fileName + ".csv");
			if (!workingFile.exists())
			{
				workingData = new ClanTracker(fileName);
			}
			else
			{
				workingData = new ClanTracker(fileName);
				readDataFile();
			}
		}

	}

	private void updateMembershipData()
	{
		//First check that the user hasn't changed clans.
		//Currently, the closest thing I see to a unique identifier is the Clan Name.
		//I will be assuming a change in clan name means the user has left and joined a different clan.
		//Perhaps I could change this to just update the clan name, and let the user manually delete their records when they change clans.
		if (client.getClanChannel() == null)
		{
			return;
		}

		if (!client.getClanChannel().getName().equals(workingData.clanName))
			initializeFileAndData();

		LocalDateTime timeUpdated = LocalDateTime.now();
		updateMembershipList(timeUpdated);
		updateOnlineMembershipData(timeUpdated);
	}

	private void updateMembershipList(LocalDateTime timeUpdated)
	{
		List<ClanMember> allMembers = client.getClanSettings().getMembers();

		//Update the tracked data with any new members
		allMembers.forEach((clanMember) ->
		{
			if (workingData.clanMembers == null)
			{
				ClanMemberTracker newMember = new ClanMemberTracker(clanMember.getName(), timeUpdated);
				workingData.clanMembers.add(newMember);
			}
			ClanMemberTracker tracker = workingData.clanMembers.stream().filter(c -> clanMember.getName().equals(c.name)).findFirst().orElse(null);
			if (tracker == null)
			{
				ClanMemberTracker newMember = new ClanMemberTracker(clanMember.getName(), timeUpdated);
				workingData.clanMembers.add(newMember);
			}
		});

		//Remove any former members from the tracked data.
		List<ClanMemberTracker> clanMembersToRemove = new ArrayList<ClanMemberTracker>();
		workingData.clanMembers.forEach((clanMemberTracker) ->
		{
			ClanMember member = allMembers.stream().filter(m -> clanMemberTracker.name.equals(m.getName())).findFirst().orElse(null);
			if (member == null)
			{
				clanMembersToRemove.add(clanMemberTracker);
			}
		});
		clanMembersToRemove.forEach((clanMemberTracker) ->
		{
			workingData.clanMembers.remove(clanMemberTracker);
		});
	}

	private void updateOnlineMembershipData(LocalDateTime timeUpdated)
	{
		List<ClanChannelMember> onlineMembers = client.getClanChannel().getMembers();

		onlineMembers.forEach((clanMember) ->
		{
			ClanMemberTracker tracker = workingData.clanMembers.stream().filter(c -> clanMember.getName().equals(c.name)).findFirst().orElse(null);
			if (tracker != null)
			{
				tracker.lastLoggedIn = timeUpdated;
			}
		});
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() == ChatMessageType.FRIENDSCHAT)
		{
			client.addChatMessage(ChatMessageType.CLAN_CHAT, "IronDravekD", "Test message", "IronDravekD");
		}
		if (chatMessage.getType() == ChatMessageType.CLAN_CHAT)
		{
			LocalDateTime timeUpdated = LocalDateTime.now();
			String chatterName = chatMessage.getName().substring(chatMessage.getName().indexOf('>') + 1);
			ClanMemberTracker tracker = workingData.clanMembers.stream().filter(c -> chatterName.equals(c.name)).findFirst().orElse(null);
			if (tracker == null)
			{
				ClanMemberTracker newMember = new ClanMemberTracker(chatMessage.getName(), timeUpdated);
				newMember.lastChattedInClan = timeUpdated;
			}
			else
			{
				tracker.lastChattedInClan = timeUpdated;
			}
		}
	}

	private void readDataFile()
	{
		try (FileInputStream in = new FileInputStream(workingFile);
			ObjectInputStream inObj = new ObjectInputStream(in))
		{
			workingData = (ClanTracker)inObj.readObject();
		}
		catch (Exception ex)
		{
			log.error(ex.toString());
		}
	}

	private void writeDataFile()
	{
		if (workingData != null && workingFile != null)
		{
			try (FileOutputStream out = new FileOutputStream(workingFile);
				 ObjectOutputStream objOut = new ObjectOutputStream(out))
			{
				objOut.writeObject(workingData);
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
			updateMembershipData();
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
		if (event.getClanChannel() != null && event.getClanId() == ClanID.CLAN)
		{
			initializeFileAndData();
		}
	}

	private void exportDataFile()
	{
		try (PrintWriter writer = new PrintWriter(exportFile, "UTF-8");
		)
		{
			writer.println("Name,LastLoggedIn,LastChattedInClan");
			workingData.clanMembers.forEach((clanMember) ->
			{
				String line = clanMember.name + ",";
				if (clanMember.lastLoggedIn != null)
					line += clanMember.lastLoggedIn.format(dateTimeFormatter) + ",";
				else
					line += "null,";
				if (clanMember.lastChattedInClan != null)
					line += clanMember.lastChattedInClan.format(dateTimeFormatter);
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

	//Not needed, the encoding was messing with the data matching correctly.
	private void readCsvFile()
	{
		try(BufferedReader br = new BufferedReader(new FileReader(workingFile))) {
			String test = br.readLine(); //Read the header line, we don't need this.
			for(String line; (line = br.readLine()) != null; ) {
				String[] lineItems = line.split(",", 0);

				LocalDateTime lastLoggedIn = null;
				LocalDateTime lastChattedInClan = null;
				if (!lineItems[1].equals("null"))
					lastLoggedIn = LocalDateTime.parse(lineItems[1], dateTimeFormatter);
				if (!lineItems[2].equals("null"))
					lastChattedInClan = LocalDateTime.parse(lineItems[2], dateTimeFormatter);

				workingData.clanMembers.add(new ClanMemberTracker(lineItems[0], lastLoggedIn, lastChattedInClan));
			}
		}
		catch (Exception ex)
		{
			log.error(ex.toString());
		}
	}
}
