package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import java.util.Date;

public class UserMessage {

    private String messageText;
    private String messageUser;
    private String photoUrl;
    private String bubbleName;
    private long bubbleId;
    private long messageTime;
    private boolean readState;

    public UserMessage(String messageText, String messageUser, String photoUrl, String bubbleName, long bubbleId, long messageTime, boolean readState) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.photoUrl = photoUrl;
        this.readState = readState;
        this.bubbleName = bubbleName;
        this.bubbleId = bubbleId;

        // Initialize to current time
        this.messageTime = new Date().getTime();
    }

    public UserMessage(){

    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean getReadState() {
        return readState;
    }

    public void setReadState(boolean readState) {
        this.readState = readState;
    }

    public String getMessageBubble() {
        return bubbleName;
    }

    public void setMessageBubble(String bubbleName) {
        this.bubbleName = bubbleName;
    }

    public long getBubbleId() {
        return bubbleId;
    }

    public void setBubbleId(long bubbleId) {
        this.bubbleId = bubbleId;
    }


}
