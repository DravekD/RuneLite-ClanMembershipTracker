package com.dravekd.clanmembershiptracker;

import java.time.LocalDateTime;

public class ClanMemberTracker implements java.io.Serializable {
    //Members
    public String name;
    public LocalDateTime lastLoggedIn;
    public LocalDateTime lastChattedInClan;

    //Methods
    public ClanMemberTracker(String memberName, LocalDateTime loggedInDate)
    {
        name = memberName;
        lastLoggedIn = loggedInDate;
    }

    public ClanMemberTracker(String memberName, LocalDateTime loggedInDate, LocalDateTime lastChattedDate)
    {
        name = memberName;
        lastLoggedIn = loggedInDate;
        lastChattedInClan = lastChattedDate;
    }
}
