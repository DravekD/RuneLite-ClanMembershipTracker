package com.dravekd.clanmembershiptracker;

import net.runelite.api.clan.ClanRank;

import java.time.LocalDateTime;

public class ClanMemberTracker implements java.io.Serializable {
    //Members
    public String DisplayName;
    public String Rank;
    public LocalDateTime LastLogDate;
    public LocalDateTime LastChatDate;

    //Methods
    public ClanMemberTracker(String memberName, ClanRank memberRank, LocalDateTime loggedInDate)
    {
        DisplayName = memberName;
        LastLogDate = loggedInDate;

        if (memberRank == ClanRank.GUEST)
            Rank = "Guest";
        else if (memberRank == ClanRank.ADMINISTRATOR)
            Rank = "Admin";
        else if (memberRank == ClanRank.DEPUTY_OWNER)
            Rank = "Deputy Owner";
        else if (memberRank == ClanRank.OWNER)
            Rank = "Owner";
        else
            Rank = "Member";
    }

    public ClanMemberTracker(String memberName, LocalDateTime lastChattedDate)
    {
        DisplayName = memberName;
        LastLogDate = lastChattedDate;
        LastChatDate = lastChattedDate;
    }
}
