package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.w(TAG, "Received Notification Title - " + remoteMessage.getNotification().getTitle());
        Log.w(TAG, "Received Notification Body-" + remoteMessage.getNotification().getBody());

        if (!remoteMessage.getNotification().getTitle().contains(FirebaseAuth.getInstance().getCurrentUser().getDisplayName())) {

            final int notificationId = new Random().nextInt(3000);

            ArrayList<String> messageTitleContents = new ArrayList<>(Arrays.asList(remoteMessage.getNotification().getTitle().split("@")));
            final String messageBubble = messageTitleContents.get(1).trim();

            Log.w(TAG, "Received Notification Bubble - " + messageBubble);

            final ArrayList<String> bubbleInfo = new ArrayList<>();

            final Query bubbleQuery = FirebaseDatabase.getInstance()
                    .getReference().child("groups")
                    .orderByChild("groupTitle")
                    .equalTo(messageBubble);

            bubbleQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Log.w(TAG, "Data Now - " + data.getValue(GroupItem.class).getGroupTitle());
                        String bubbleNameForInfo = data.getValue(GroupItem.class).getGroupTitle();
                        long bubbleIdForInfo = data.getValue(GroupItem.class).getGroupId();

                        bubbleInfo.add(bubbleNameForInfo);
                        bubbleInfo.add(String.valueOf(bubbleIdForInfo));

                        Log.w(TAG, "Info Now - " + bubbleInfo);

                        Intent intent = new Intent(MyFirebaseMessagingService.this, ChatActivity.class);
                        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                        PendingIntent pendingIntent = PendingIntent.getActivity(MyFirebaseMessagingService.this, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        String channelId = getString(R.string.default_notification_channel_id);
                        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        NotificationCompat.Builder notificationBuilder =
                                new NotificationCompat.Builder(MyFirebaseMessagingService.this, channelId)
                                        .setSmallIcon(R.drawable.chat)
                                        .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent))
                                        .setColorized(true)
                                        .setContentTitle(remoteMessage.getNotification().getTitle())
                                        .setContentText(remoteMessage.getNotification().getBody())
                                        .setAutoCancel(true)
                                        .setSound(defaultSoundUri)
                                        .setContentIntent(pendingIntent);

                        NotificationManager notificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                        // Since android Oreo notification channel is needed.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            NotificationChannel channel = new NotificationChannel(channelId,
                                    "Babbling Bubble",
                                    NotificationManager.IMPORTANCE_DEFAULT);
                            notificationManager.createNotificationChannel(channel);
                        }

                        notificationManager.notify(notificationId, notificationBuilder.build());

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ItemDetail", "onCancelled", databaseError.toException());
                }
            });


        } else {
            //Do nothing, or else you will be sending self-notifications
        }


    }

}
