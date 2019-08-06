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
import android.text.InputFilter;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TodoActivity extends AppCompatActivity {

    private ListView listOfTodos;

    private FirebaseListAdapter<TodoItem> adapter;
    private ArrayList<String> bubbleInfo;

    private UserItem user;

    private int notificationId = 1228;
    private static final int SIGN_IN_REQUEST_CODE = 2812;
    private static final int todoLength = 140;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo);

        listOfTodos = findViewById(R.id.list_of_todos);

        displayTodos();

        FirebaseApp.getInstance();
        new FirebaseHandler();
        FirebaseDatabase.getInstance().getReference().keepSynced(true);

        final Intent intentAction = getIntent();
        String action = intentAction.getAction();
        String type = intentAction.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                receivedSharedTextHandler(intentAction); // Handle text being sent
            }
        } else if (Intent.EXTRA_KEY_EVENT.equals(action) && type.equals("text/plain")) {

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                // Start sign in/sign up activity
                List<AuthUI.IdpConfig> providers = Arrays.asList(
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

            } else {

                // User is already signed in.

                Intent intent = getIntent();
                bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");
                setTitle(bubbleInfo.get(0) + " - Tasks");

                final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                final String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                final String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                final ArrayList<String> groupArray = new ArrayList<>();

                final Query groupToDisplay = FirebaseDatabase.getInstance()
                        .getReference().child("groups")
                        .orderByChild("peopleInGroup");

                groupToDisplay.addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot group : dataSnapshot.getChildren()) {
                            Log.w("GroupsActivity", "bello listed - " + group);
                            if (Objects.requireNonNull(group.getValue(GroupItem.class)).getPeopleInGroup().contains(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
                                groupArray.add(Objects.requireNonNull(group.getValue(GroupItem.class)).getGroupTitle());
                                Log.w("GroupsActivity", "bello added - " + group);
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
                                        itemToUpdateSnapshot.getValue(UserItem.class)
                                                .setUserDisplayName(user.getUserDisplayName());
                                        itemToUpdateSnapshot.getValue(UserItem.class)
                                                .setUserEmailAddress(user.getUserEmailAddress());
                                        itemToUpdateSnapshot.getValue(UserItem.class)
                                                .setUserGroups(user.getUserGroups());
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

                // Load todos
                displayTodos();
            }

            final Button addTaskButton = findViewById(R.id.add_task_button);

            addTaskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent intent = getIntent();
                    bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");
                    Log.w("TodoActivity", "Bello 2 - " + bubbleInfo);

                    final EditText addTaskEditText = findViewById(R.id.add_task_box);
                    addTaskEditText.setText(intentAction.getStringExtra("textToShare"));
                    addTaskEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(todoLength)});

                    final long todoId = UUID.randomUUID().getLeastSignificantBits();

                    if (addTaskEditText.getText().toString().trim().length() > 0) {

                        Log.w("TodoActivity", "Bello - " + bubbleInfo);

                        if (bubbleInfo != null) {

                            final Query messageToSend = FirebaseDatabase.getInstance()
                                    .getReference().child("groups")
                                    .orderByChild("groupId")
                                    .equalTo(Long.parseLong(bubbleInfo.get(1)));

                            messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                                .push()
                                                .setValue(new ChatMessage("I have added the " +
                                                        "task " + addTaskEditText.getText().toString() + " !",
                                                        FirebaseAuth.getInstance()
                                                                .getCurrentUser()
                                                                .getDisplayName(),
                                                        null)
                                                );

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("messages")
                                                .push()
                                                .setValue(new UserMessage("I have added the " +
                                                        "task " + addTaskEditText.getText().toString() + " !",
                                                        FirebaseAuth.getInstance()
                                                                .getCurrentUser()
                                                                .getDisplayName(),
                                                        null,
                                                        bubbleInfo.get(0),
                                                        Long.valueOf(bubbleInfo.get(1)),
                                                        new Date().getTime(),
                                                        false)
                                                );

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("todos")
                                                .push()
                                                .setValue(new TodoItem(addTaskEditText.getText().toString(),
                                                        null, todoId)
                                                );

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                                .push()
                                                .setValue(new ChatMessage("I have added the " +
                                                        "task " + addTaskEditText.getText().toString() + " !",
                                                        FirebaseAuth.getInstance()
                                                                .getCurrentUser()
                                                                .getDisplayName(),
                                                        null)
                                                );

                                        // Clear the addTaskEditText
                                        addTaskEditText.setText("");

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
                        // Do nothing
                    }
                }
            });


            listOfTodos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String itemTitle = adapter.getItem(position).getTodoTitle();
                    String itemClaimedBy = adapter.getItem(position).getClaimedBy();
                    long itemInitTime = adapter.getItem(position).getTodoInitTime();

                    Log.w("OnClickItemInList ", "Claimed By " + itemClaimedBy);

                    Intent intent2 = getIntent();
                    bubbleInfo = intent2.getStringArrayListExtra("bubbleInfo");

                    ArrayList<String> todoItemInfo = new ArrayList<>();
                    todoItemInfo.add(itemTitle);
                    todoItemInfo.add(itemClaimedBy);
                    todoItemInfo.add(String.valueOf(itemInitTime));
                    todoItemInfo.add(String.valueOf(adapter.getItem(position).getTodoItemId()));

                    Log.w("TodoActivity", "Bello - " + bubbleInfo);
                    todoItemInfo.add(String.valueOf(bubbleInfo.get(0)));
                    todoItemInfo.add(String.valueOf(bubbleInfo.get(1)));

                    Intent intent = new Intent(TodoActivity.this, ItemDetail.class);
                    intent.putStringArrayListExtra("todoItemInfo", todoItemInfo);
                    intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                    startActivity(intent);
                }
            });
        } else {

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                // Start sign in/sign up activity
                List<AuthUI.IdpConfig> providers = Arrays.asList(
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

            } else {

                // User is already signed in.

                Intent intent = getIntent();
                bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");
                setTitle(bubbleInfo.get(0) + " - Tasks");

                final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                final String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                final String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                final ArrayList<String> groupArray = new ArrayList<>();

                final Query groupToDisplay = FirebaseDatabase.getInstance()
                        .getReference().child("groups")
                        .orderByChild("peopleInGroup");

                groupToDisplay.addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot group : dataSnapshot.getChildren()) {
                            Log.w("GroupsActivity", "bello listed - " + group);
                            if (Objects.requireNonNull(group.getValue(GroupItem.class)).getPeopleInGroup().contains(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
                                groupArray.add(Objects.requireNonNull(group.getValue(GroupItem.class)).getGroupTitle());
                                Log.w("GroupsActivity", "bello added - " + group);
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
                                        itemToUpdateSnapshot.getValue(UserItem.class)
                                                .setUserDisplayName(user.getUserDisplayName());
                                        itemToUpdateSnapshot.getValue(UserItem.class)
                                                .setUserEmailAddress(user.getUserEmailAddress());
                                        itemToUpdateSnapshot.getValue(UserItem.class)
                                                .setUserGroups(user.getUserGroups());
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

                // Load todos
                displayTodos();
            }

            final Button addTaskButton = findViewById(R.id.add_task_button);

            addTaskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent intent = getIntent();
                    bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");
                    Log.w("TodoActivity", "Bello 2 - " + bubbleInfo);

                    final EditText addTaskEditText = findViewById(R.id.add_task_box);
                    addTaskEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(todoLength)});

                    final long todoId = UUID.randomUUID().getLeastSignificantBits();

                    if (addTaskEditText.getText().toString().trim().length() > 0) {

                        Log.w("TodoActivity", "Bello - " + bubbleInfo);

                        if (bubbleInfo != null) {

                            final Query messageToSend = FirebaseDatabase.getInstance()
                                    .getReference().child("groups")
                                    .orderByChild("groupId")
                                    .equalTo(Long.parseLong(bubbleInfo.get(1)));

                            messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                                .push()
                                                .setValue(new ChatMessage("I have added the " +
                                                        "task " + addTaskEditText.getText().toString() + " !",
                                                        FirebaseAuth.getInstance()
                                                                .getCurrentUser()
                                                                .getDisplayName(),
                                                        null)
                                                );

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("messages")
                                                .push()
                                                .setValue(new UserMessage("I have added the " +
                                                        "task " + addTaskEditText.getText().toString() + " !",
                                                        FirebaseAuth.getInstance()
                                                                .getCurrentUser()
                                                                .getDisplayName(),
                                                        null,
                                                        bubbleInfo.get(0),
                                                        Long.valueOf(bubbleInfo.get(1)),
                                                        new Date().getTime(),
                                                        false)
                                                );

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("todos")
                                                .push()
                                                .setValue(new TodoItem(addTaskEditText.getText().toString(),
                                                        null, todoId)
                                                );

                                        // Clear the addTaskEditText
                                        addTaskEditText.setText("");

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
                        // Do nothing
                    }
                }
            });


            listOfTodos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String itemTitle = adapter.getItem(position).getTodoTitle();
                    String itemClaimedBy = adapter.getItem(position).getClaimedBy();
                    long itemInitTime = adapter.getItem(position).getTodoInitTime();

                    Log.w("OnClickItemInList ", "Claimed By " + itemClaimedBy);

                    Intent intent2 = getIntent();
                    bubbleInfo = intent2.getStringArrayListExtra("bubbleInfo");

                    ArrayList<String> todoItemInfo = new ArrayList<>();
                    todoItemInfo.add(itemTitle);
                    todoItemInfo.add(itemClaimedBy);
                    todoItemInfo.add(String.valueOf(itemInitTime));
                    todoItemInfo.add(String.valueOf(adapter.getItem(position).getTodoItemId()));

                    Log.w("TodoActivity", "Bello - " + bubbleInfo);
                    todoItemInfo.add(String.valueOf(bubbleInfo.get(0)));
                    todoItemInfo.add(String.valueOf(bubbleInfo.get(1)));

                    Intent intent = new Intent(TodoActivity.this, ItemDetail.class);
                    intent.putStringArrayListExtra("todoItemInfo", todoItemInfo);
                    intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                    startActivity(intent);
                }
            });
        }
    }

    private void displayTodos() {
        final ListView listOfTodos = findViewById(R.id.list_of_todos);

        Intent intent = getIntent();
        final ArrayList<String> bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");

        Log.w("TodoActivity", "Bello - " + bubbleInfo);

        if (bubbleInfo != null) {

            final Query messageToSend = FirebaseDatabase.getInstance()
                    .getReference().child("groups")
                    .orderByChild("groupId")
                    .equalTo(Long.parseLong(bubbleInfo.get(1)));

            messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                        adapter = new FirebaseListAdapter<TodoItem>(TodoActivity.this, TodoItem.class,
                                R.layout.todo_list_item, FirebaseDatabase.getInstance()
                                .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("todos")) {

                            @Override
                            protected void populateView(View v, TodoItem model, int position) {
                                TextView taskClaimedByTextView = v.findViewById(R.id.task_claimed_by);
                                TextView taskTitleTextView = v.findViewById(R.id.task_title);
                                TextView taskTimeTextView = v.findViewById(R.id.task_time_set);

                                // Set the data for all tasks
                                taskClaimedByTextView.setText(model.getClaimedBy());
                                taskTitleTextView.setText(model.getTodoTitle());
                                taskTimeTextView.setText(DateFormat.format("dd-MM-yyyy @ HH:mm",
                                        model.getTodoInitTime()));
                            }
                        };

                        listOfTodos.setAdapter(adapter);

                        listOfTodos.smoothScrollToPosition(adapter.getCount() - 1);

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

    private void receivedSharedTextHandler(Intent intentAction) {
        String sharedText = intentAction.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            Intent intent = new Intent(TodoActivity.this, SharedTextHandler.class);
            intent.putExtra("textToShare", sharedText);
            startActivity(intent);
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

    private void SendNotificationForPhoto(String messageUser) {

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SIGN_IN_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(this,
                        "You're successfully signed in. Welcome! :)",
                        Toast.LENGTH_LONG)
                        .show();
                displayTodos();
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
        getMenuInflater().inflate(R.menu.group_menu, menu);
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
                            Toast.makeText(TodoActivity.this,
                                    "You have been signed out. Come back soon!",
                                    Toast.LENGTH_LONG)
                                    .show();

                            // Close activity
                            List<AuthUI.IdpConfig> providers = Arrays.asList(
                                    new AuthUI.IdpConfig.EmailBuilder().build());

                            // Create and launch sign-in intent
                            startActivityForResult(
                                    AuthUI.getInstance()
                                            .createSignInIntentBuilder()
                                            .setAvailableProviders(providers)
                                            .build(),
                                    SIGN_IN_REQUEST_CODE);
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
                                        .delete(TodoActivity.this)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(TodoActivity.this,
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
        } else if (item.getItemId() == R.id.menu_add_member) {

            Intent intent2 = getIntent();
            final ArrayList<String> bubbleInfo = intent2.getStringArrayListExtra("bubbleInfo");

            Intent intent = new Intent(TodoActivity.this, AddFriend.class);
            intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
            startActivity(intent);

        } else if (item.getItemId() == R.id.legal) {

            Intent intent = new Intent(TodoActivity.this, LegalActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == R.id.menu_group_info) {

            Intent intentToReceive = getIntent();
            ArrayList<String> bubbleInfo = new ArrayList<>(intentToReceive.getStringArrayListExtra("bubbleInfo"));

            Intent intent = new Intent(TodoActivity.this, GroupInfoActivity.class);
            intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
            startActivity(intent);

        /*} else if (item.getItemId() == R.id.menu_delete_group) {

            Intent intent2 = getIntent();
            final ArrayList<String> bubbleInfo = intent2.getStringArrayListExtra("bubbleInfo");

            final Query userToRemoveFromGroup = FirebaseDatabase.getInstance()
                    .getReference().child("groups")
                    .orderByChild("groupId")
                    .equalTo(bubbleInfo.get(1));

            userToRemoveFromGroup.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot emailToRemove : dataSnapshot.getChildren()) {

                        ArrayList<String> groupInfoArray = new ArrayList<>(emailToRemove.getValue(GroupItem.class).getPeopleInGroup());

                         groupInfoArray.remove(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                        emailToRemove.getValue(GroupItem.class).setPeopleInGroup(groupInfoArray);

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ItemDetail", "onCancelled", databaseError.toException());
                }
            });

            final Query groupToRemoveFromUser = FirebaseDatabase.getInstance()
                    .getReference().child("users")
                    .orderByChild("userGroups")
                    .equalTo(bubbleInfo.get(0));

            groupToRemoveFromUser.addValueEventListener(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot emailToRemove : dataSnapshot.getChildren()) {

                        Toast.makeText(TodoActivity.this, "Yay!" + emailToRemove, Toast.LENGTH_SHORT).show();

                        ArrayList<String> userGroupsArray = new ArrayList<>(emailToRemove.getValue(UserItem.class).getUserGroups());

                        userGroupsArray.remove(Objects.requireNonNull(bubbleInfo.get(0)));

                        emailToRemove.getValue(UserItem.class).setUserGroups(userGroupsArray);

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ItemDetail", "onCancelled", databaseError.toException());
                }
            });


            Intent intent = new Intent(TodoActivity.this, GroupsActivity.class);
            startActivity(intent);
        */
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }
}