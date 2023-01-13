package com.dravekd.clanmembershiptracker;

import net.runelite.api.clan.ClanRank;

import java.time.LocalDateTime;

public class ClanMemberTracker implements java.io.Serializable {
    //Members
    public String displayName;
    public String rank;
    public LocalDateTime lastLogDate;
    public LocalDateTime lastChatDate;

    //Methods
    public ClanMemberTracker(String memberName, ClanRank memberRank, LocalDateTime loggedInDate)
    {
        displayName = memberName;
        lastLogDate = loggedInDate;

        if (memberRank == ClanRank.GUEST)
            rank = "Guest";
        else if (memberRank == ClanRank.ADMINISTRATOR)
            rank = "Admin";
        else if (memberRank == ClanRank.DEPUTY_OWNER)
            rank = "Deputy Owner";
        else if (memberRank == ClanRank.OWNER)
            rank = "Owner";
        else
            rank = "Member";
    }

    public ClanMemberTracker(String memberName, LocalDateTime lastChattedDate)
    {
        displayName = memberName;
        lastLogDate = lastChattedDate;
        lastChatDate = lastChattedDate;
    }
}
