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
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

public class ItemDetail extends AppCompatActivity {

    private int notificationId = 2128;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        Intent intent = getIntent();
        ArrayList<String> todoItemInfoReceived = intent.getStringArrayListExtra("todoItemInfo");

        final String itemTitleReceived = todoItemInfoReceived.get(0);
        final String itemClaimedByReceived = todoItemInfoReceived.get(1);
        final long itemInitTimeReceived = Long.parseLong(todoItemInfoReceived.get(2));
        final long claimedItemId = Long.parseLong(todoItemInfoReceived.get(3));

        final ArrayList<String> bubbleInfo = new ArrayList<>();
        bubbleInfo.add(todoItemInfoReceived.get(4));
        bubbleInfo.add(todoItemInfoReceived.get(5));

        Log.w("ItemDetail", "Item Info- " + bubbleInfo);

        final TextView taskTitleTextView = findViewById(R.id.task_title);
        final TextView taskClaimedByTextView = findViewById(R.id.task_claimed_by);
        final TextView taskTimeTextView = findViewById(R.id.task_time_set);

        // Set the data for task
        taskClaimedByTextView.setText(itemClaimedByReceived);
        taskTitleTextView.setText(itemTitleReceived);
        // Format the date before showing it
        taskTimeTextView.setText(DateFormat.format("dd-MM-yyyy @ HH:mm",
                itemInitTimeReceived));

        Button claimTaskButton = findViewById(R.id.claim_task_button);

        if (itemClaimedByReceived == null) {
            claimTaskButton.setEnabled(true);
            claimTaskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    taskClaimedByTextView.setText(FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getDisplayName());

                    final String claimedByAfterButtonClick = FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getDisplayName();

                    Log.w("ItemDetail", "Bello - " + bubbleInfo);
                    if (bubbleInfo != null) {

                        final Query messageToSend = FirebaseDatabase.getInstance()
                                .getReference().child("groups")
                                .orderByChild("groupId")
                                .equalTo(Long.parseLong(bubbleInfo.get(1)));

                        messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (final DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                                    FirebaseDatabase.getInstance()
                                            .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                            .push()
                                            .setValue(new ChatMessage("I have claimed the " +
                                                    "task " + itemTitleReceived + " !",
                                                    FirebaseAuth.getInstance()
                                                            .getCurrentUser()
                                                            .getDisplayName(),
                                                    null)
                                            );

                                    FirebaseDatabase.getInstance()
                                            .getReference().child("messages")
                                            .push()
                                            .setValue(new UserMessage("I have claimed the " +
                                                    "task " + itemTitleReceived + " !",
                                                    FirebaseAuth.getInstance()
                                                            .getCurrentUser()
                                                            .getDisplayName(),
                                                    null,
                                                    bubbleInfo.get(0),
                                                    Long.valueOf(bubbleInfo.get(1)),
                                                    new Date().getTime(),
                                                    false)
                                            );

                                    final Query itemToRemoveId = FirebaseDatabase.getInstance()
                                            .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("todos")
                                            .orderByChild("todoItemId")
                                            .equalTo(claimedItemId);

                                    itemToRemoveId.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            for (DataSnapshot itemToRemoveSnapshot : dataSnapshot.getChildren()) {
                                                FirebaseDatabase.getInstance()
                                                        .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("todos")
                                                        .push()
                                                        .setValue(new TodoItem(itemTitleReceived,
                                                                claimedByAfterButtonClick, claimedItemId)
                                                        );

                                                itemToRemoveSnapshot.getRef().removeValue();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            Log.e("ItemDetail", "onCancelled", databaseError.toException());
                                        }
                                    });

                                    Intent intent = new Intent(ItemDetail.this, TodoActivity.class);
                                    intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                                    startActivity(intent);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("ItemDetail", "onCancelled", databaseError.toException());
                            }
                        });
                    } else {
                        // Do nothing
                    }
                }
            });
        } else {
            claimTaskButton.setEnabled(false);
        }

        final Button completedTaskButton = findViewById(R.id.completed_task_button);

        if (itemClaimedByReceived != null && itemClaimedByReceived.equals(FirebaseAuth
                                                .getInstance().getCurrentUser().getDisplayName())) {

            Log.w("ItemDetail", "Bello - " + bubbleInfo);
            if (bubbleInfo != null) {
                //final long bubbleId = Long.parseLong(bubbleInfo.get(1));

                final Query messageToSend2 = FirebaseDatabase.getInstance()
                        .getReference().child("groups")
                        .orderByChild("groupId")
                        .equalTo(Long.parseLong(bubbleInfo.get(1)));

                messageToSend2.addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (final DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                            completedTaskButton.setEnabled(true);
                            completedTaskButton.setOnClickListener(new View.OnClickListener() {

                                @Override
                                public void onClick(View view) {

                                    Toast.makeText(ItemDetail.this,
                                            "Yay!! You completed the task " + itemTitleReceived + " !",
                                            Toast.LENGTH_LONG)
                                            .show();

                                    FirebaseDatabase.getInstance()
                                            .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                            .push()
                                            .setValue(new ChatMessage("Yay!! I completed" +
                                                    " the task " + itemTitleReceived + " !",
                                                    FirebaseAuth.getInstance()
                                                            .getCurrentUser()
                                                            .getDisplayName(),
                                                    null)
                                            );

                                    FirebaseDatabase.getInstance()
                                            .getReference().child("messages")
                                            .push()
                                            .setValue(new UserMessage("Yay!! I completed" +
                                                    " the task " + itemTitleReceived + " !",
                                                    FirebaseAuth.getInstance()
                                                            .getCurrentUser()
                                                            .getDisplayName(),
                                                    null,
                                                    bubbleInfo.get(0),
                                                    Long.valueOf(bubbleInfo.get(1)),
                                                    new Date().getTime(),
                                                    false)
                                            );

                                    Query itemCompletedQuery = FirebaseDatabase.getInstance()
                                            .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("todos")
                                            .orderByChild("todoItemId")
                                            .equalTo(claimedItemId);

                                    itemCompletedQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            for (DataSnapshot itemCompletedSnapshot : dataSnapshot.getChildren()) {
                                                itemCompletedSnapshot.getRef().removeValue();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Log.e("ItemDetail", "onCancelled", databaseError.toException());
                                        }
                                    });

                                    Intent intent = new Intent(ItemDetail.this, TodoActivity.class);
                                    intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                                    startActivity(intent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                    }
                });
            } else {
                // Do nothing
            }

        } else {
            completedTaskButton.setEnabled(false);
        }

    }

    private void SendNotificationForMessage(String messageBody, String messageUser) {

        notificationId += 1;

        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.chat)
                        .setContentTitle("Message Received From " + messageUser)
                        .setContentText(messageBody)
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_sign_out) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {
                            Toast.makeText(ItemDetail.this,
                                    "You have been signed out. Come back soon!",
                                    Toast.LENGTH_LONG)
                                    .show();

                            // Close activity
                            finish();
                        }
                    });
        } else if (item.getItemId() == R.id.menu_delete_account) {

            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                //You need to get here the token you saved at logging-in time.
                String token = "userSavedToken";
                //You need to get here the password you saved at logging-in time.
                String password = "userSavedPassword";

                AuthCredential credential;

                //This means you didn't have the token because user used like Facebook Sign-in method.
                if (token == null) {
                    credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                } else {
                    //Doesn't matter if it was Facebook Sign-in or others. It will always work using GoogleAuthProvider for whatever the provider.
                    credential = GoogleAuthProvider.getCredential(token, null);
                }

                user.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull com.google.android.gms.tasks.Task<Void> task) {

                                //Calling delete to remove the user and wait for a result.
                                final Query userNodeToRemove = FirebaseDatabase.getInstance()
                                        .getReference().child("users")
                                        .orderByChild("userEmailAddress")
                                        .equalTo(FirebaseAuth.getInstance().getCurrentUser().getEmail());

                                userNodeToRemove.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot nodeToRemove : dataSnapshot.getChildren()) {
                                            Log.w("GroupsActivity", "Removed Node - " + nodeToRemove);
                                            nodeToRemove.getRef().removeValue();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                                    }
                                });

                                // Finally deleting the user from FirebaseAuth
                                AuthUI.getInstance()
                                        .delete(ItemDetail.this)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(ItemDetail.this,
                                                            "Your account has been deleted successfully. " +
                                                                    "Please come back with a new account!! :(",
                                                            Toast.LENGTH_LONG)
                                                            .show();

                                                    // Close activity
                                                    finish();
                                                } else {
                                                    task.getException();
                                                }
                                            }
                                        });

                            }
                        });
            }
        } else if (item.getItemId() == R.id.legal) {

            Intent intent = new Intent(ItemDetail.this, LegalActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == android.R.id.home) {
            Intent intentReceived = getIntent();
            ArrayList<String> bubbleInfo = intentReceived.getStringArrayListExtra("bubbleInfo");

            Intent intent = new Intent(ItemDetail.this, TodoActivity.class);
            intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
            startActivity(intent);
        }
        return true;
    }
}

