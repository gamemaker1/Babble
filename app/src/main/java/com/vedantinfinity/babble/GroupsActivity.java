package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
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
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GroupsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listOfGroups;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> userGroupsArray;

    private UserItem user;

    private static final int SIGN_IN_REQUEST_CODE = 2812;
    private int notificationId = 2812;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        listOfGroups = findViewById(R.id.list_of_groups);

        FirebaseApp.getInstance();
        new FirebaseHandler();
        FirebaseDatabase.getInstance().getReference().keepSynced(true);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Start sign in/sign up activity
            List<AuthUI.IdpConfig> providers = Collections.singletonList(
                    new AuthUI.IdpConfig.EmailBuilder().build());

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setLogo(R.drawable.chat)
                            .setTheme(R.style.AppTheme)
                            .build(),
                    SIGN_IN_REQUEST_CODE);

            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w("ChatActivity", "getInstanceId failed", task.getException());
                                return;
                            }

                            // Get new Instance ID token
                            String token = task.getResult().getToken();

                            // Log and toast
                            String msg = token;
                            Log.w("ChatActivity", msg);
                        }
                    });

            displayGroups();

        } else {

            // User is already signed in.

            //sendNotifications();

            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w("ChatActivity", "getInstanceId failed", task.getException());
                            }

                            // Get new Instance ID token

                            // Log and toast
                            String msg = task.getResult().getToken();
                            Log.w("GroupActivity", msg);
                        }
                    });

            for (int i = 0; i < 10; i++) {
                Log.w("GroupsActivity", "SIGNED IN NOW");
            }

            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            final String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            final String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            final ArrayList<String> groupArray = new ArrayList<>();

            final Query groupToDisplay = FirebaseDatabase.getInstance()
                    .getReference().child("groups")
                    .orderByChild("peopleInGroup");

            groupToDisplay.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot group : dataSnapshot.getChildren()) {
                        Log.w("GroupsActivity", "bello listed - " + group);
                        if (Objects.requireNonNull(group.getValue(GroupItem.class)).getPeopleInGroup().contains(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
                            groupArray.add(Objects.requireNonNull(group.getValue(GroupItem.class)).getGroupTitle());
                            FirebaseMessaging.getInstance().subscribeToTopic(Objects.requireNonNull(group.getValue(GroupItem.class)).getGroupTitle().replaceAll("\\s", ""));
                            Log.w("GroupsActivity", "bello added - " + group.getValue(GroupItem.class).getPeopleInGroup());
                            Log.w("GroupsActivity", "bello added - " + groupArray);
                        }
                    }

                    Log.w("GroupsActivity", "TEST TEST TEST 11 - " + groupArray);
                    user = new UserItem(displayName, email, uid, groupArray);
                    Log.w("GroupsActivity", "TEST TEST TEST 33 - " + user);

                    final Query userToAdd = FirebaseDatabase.getInstance()
                            .getReference().child("users")
                            .orderByChild("uid")
                            .equalTo(user.getUid());

                    userToAdd.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                for (DataSnapshot itemToUpdateSnapshot : dataSnapshot.getChildren()) {

                                    itemToUpdateSnapshot.getRef().removeValue();

                                    FirebaseDatabase.getInstance()
                                            .getReference()
                                            .child("users")
                                            .push().setValue(user);
                                }
                            } else {
                                FirebaseDatabase.getInstance().getReference()
                                        .child("users")
                                        .push().setValue(user);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("ItemDetail", "onCancelled", databaseError.toException());
                        }
                    });

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ItemDetail", "onCancelled", databaseError.toException());
                }
            });

            // Load groups
            displayGroups();
        }

        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intentReceived = getIntent();
                ArrayList<String> bubbleInfo = intentReceived.getStringArrayListExtra("bubbleInfo");

                Intent intent = new Intent(GroupsActivity.this, GroupCreate.class);
                intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                startActivity(intent);
            }

        });

        listOfGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final ArrayList<String> bubbleInfo = new ArrayList<>();

                final Query bubbleQuery = FirebaseDatabase.getInstance()
                        .getReference().child("groups")
                        .orderByChild("groupTitle")
                        .equalTo(userGroupsArray.get(position));

                bubbleQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            Log.w("GroupsActivity", "Data Now - " + data.getValue(GroupItem.class).getGroupTitle());
                            String bubbleNameForInfo = data.getValue(GroupItem.class).getGroupTitle();
                            long bubbleIdForInfo = data.getValue(GroupItem.class).getGroupId();

                            bubbleInfo.add(bubbleNameForInfo);
                            bubbleInfo.add(String.valueOf(bubbleIdForInfo));

                            Log.w("GroupsActivity", "Info Now - " + bubbleInfo);

                        }

                        Log.w("GroupsActivity", "Info 2 Now - " + bubbleInfo);
                        Intent intent = new Intent(GroupsActivity.this, ChatActivity.class);
                        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                        startActivity(intent);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                    }
                });

            }

        });
    }

    /*@TargetApi(Build.VERSION_CODES.O)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void sendNotifications(){
        ComponentName component = new ComponentName(this, NotificationSender.class);
        JobInfo jobInfo = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            jobInfo = new JobInfo.Builder(15, component)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build();
        }

        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);
        if(resultCode == JobScheduler.RESULT_SUCCESS){
            // Yay! Message Sent!
        } else {
            Toast.makeText(GroupsActivity.this, "Sorry, your friends could not be notified of this message! :(",Toast.LENGTH_LONG).show();
        }

    }*/

    private void receivedSharedTextHandler(Intent intentAction) {
        String sharedText = intentAction.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            Intent intent = new Intent(GroupsActivity.this, SharedTextHandler.class);
            intent.putExtra("textToShare", sharedText);
            startActivity(intent);
        }
    }

    private void SendNotificationForMessage(String messageBody, String messageUser) {

        notificationId += 1;

        Intent intentReceived = getIntent();
        final ArrayList<String> bubbleInfo = intentReceived.getStringArrayListExtra("bubbleInfo");

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
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


    private void SendNotificationForPhoto(String messageUser) {

        notificationId += 1;

        Intent intentReceived = getIntent();
        final ArrayList<String> bubbleInfo = intentReceived.getStringArrayListExtra("bubbleInfo");

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.chat)
                        .setContentTitle("Photo Received From " + messageUser)
                        .setContentText("Photo")
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


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void displayGroups() {

        userGroupsArray = new ArrayList<>();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            progressBar.setVisibility(View.VISIBLE);

            final Query groupsQuery = FirebaseDatabase.getInstance()
                    .getReference().child("users")
                    .orderByChild("uid")
                    .equalTo(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

            groupsQuery.addValueEventListener(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int i = 0;
                    for (DataSnapshot bubbleName : dataSnapshot.getChildren()) {

                        Log.w("GroupsActivity", "BubbleName Now DEBUG  - " + i + "   " + dataSnapshot.getChildren().iterator().toString());
                        Log.w("GroupsActivity", "BubbleName Now - " + i + "   " + bubbleName.getValue(UserItem.class).getUserGroups());

                        if (Objects.requireNonNull(bubbleName.getValue(UserItem.class)).getUserGroups() != null) {

                            userGroupsArray.addAll(Objects.requireNonNull(bubbleName.getValue(UserItem.class)).getUserGroups());

                        } else {
                            // Do Nothing
                        }

                        i++;

                    }

                    Log.w("GroupsActivity", "ArrayList Now - " + userGroupsArray);

                    ArrayList<String> refinedBubbles = new ArrayList<>();

                    for (String refinedBubbleName : userGroupsArray) {
                        if (!refinedBubbles.contains(refinedBubbleName)) {
                            refinedBubbles.add(refinedBubbleName);
                            Log.w("GroupsActivity", "Refined List I Now - " + refinedBubbles);
                        }
                    }

                    Log.w("GroupsActivity", "Refined List O Now - " + refinedBubbles);

                    adapter = new ArrayAdapter<>(GroupsActivity.this, R.layout.group_item,
                            refinedBubbles);

                    adapter.notifyDataSetChanged();

                    listOfGroups.setAdapter(adapter);

                    progressBar.setVisibility(View.INVISIBLE);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ItemDetail", "onCancelled", databaseError.toException());
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SIGN_IN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this,
                        "You're successfully signed in. Welcome! :)",
                        Toast.LENGTH_LONG)
                        .show();
                displayGroups();

            } else {
                Toast.makeText(this,
                        "Sorry, we couldn't sign you in. Please try again later. :(",
                        Toast.LENGTH_LONG)
                        .show();

                // Close the app
                finish();
            }
        }
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
                            Toast.makeText(GroupsActivity.this,
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
                                    .delete(GroupsActivity.this)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(GroupsActivity.this,
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

        }
        return true;
    }
}
