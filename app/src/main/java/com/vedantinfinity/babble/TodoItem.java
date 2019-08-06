package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import java.util.Date;

public class TodoItem {

    private String todoTitle;
    private String claimedBy;
    private long todoInitTime;
    private long itemId;

    public TodoItem(String todoTitle, String claimedBy, long itemId) {
        this.todoTitle = todoTitle;
        this.claimedBy = claimedBy;
        this.itemId = itemId;

        // Initialize to current time
        todoInitTime = new Date().getTime();
    }

    public TodoItem(){

    }

    public String getTodoTitle() {
        return todoTitle;
    }

    public void setTodoTitle(String todoTitle) {
        this.todoTitle = todoTitle;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public long getTodoInitTime() {
        return todoInitTime;
    }

    public void setTodoInitTime(long todoInitTime) {
        this.todoInitTime = todoInitTime;
    }

    public long getTodoItemId() {
        return itemId;
    }

    public void setTodoItemId(long itemId) {
        this.itemId = itemId;
    }

}