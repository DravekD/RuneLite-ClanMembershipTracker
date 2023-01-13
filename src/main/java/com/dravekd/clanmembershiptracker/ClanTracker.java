package com.dravekd.clanmembershiptracker;

import java.util.ArrayList;
import java.util.List;

public class ClanTracker implements java.io.Serializable {
    //Members
    public String ClanName;
    public List<ClanMemberTracker> ClanMembers;

    //Methods
    public ClanTracker(String name)
    {
        ClanName = name;
        ClanMembers = new ArrayList<ClanMemberTracker>();
    }
}
