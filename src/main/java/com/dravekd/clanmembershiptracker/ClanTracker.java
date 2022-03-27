package com.dravekd.clanmembershiptracker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClanTracker implements java.io.Serializable {
    //Members
    public String clanName;
    public List<ClanMemberTracker> clanMembers;

    //Methods
    public ClanTracker(String name)
    {
        clanName = name;
        clanMembers = new ArrayList<ClanMemberTracker>();
    }
}
