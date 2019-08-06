package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import java.util.ArrayList;

public class UserItem {
    private String userDisplayName;
    private String userEmailAddress;
    private String uid;
    private ArrayList<String> groupsArray;

    public UserItem(String userDisplayName, String userEmailAddress, String uid, ArrayList<String> groupsArray) {
        this.userDisplayName = userDisplayName;
        this.userEmailAddress = userEmailAddress;
        this.uid = uid;
        this.groupsArray = groupsArray;

    }
    public UserItem() {

    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public String getUserEmailAddress() {
        return userEmailAddress;
    }

    public void setUserEmailAddress(String userEmailAddress) {
        this.userEmailAddress = userEmailAddress;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public ArrayList<String> getUserGroups() {
        return groupsArray;
    }

    public void setUserGroups(ArrayList<String> groupsArray) {
        this.groupsArray = groupsArray;
    }
}
