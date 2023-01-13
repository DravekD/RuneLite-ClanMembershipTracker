package com.dravekd.clanmembershiptracker;

import java.util.ArrayList;
import java.util.List;

public class ClanTracker implements java.io.Serializable {
    //Members
    public String name;
    public List<ClanMemberTracker> clanMembers;

    //Methods
    public ClanTracker(String name)
    {
        this.name = name;
        clanMembers = new ArrayList<ClanMemberTracker>();
    }
}
