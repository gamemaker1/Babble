package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.Continuation;
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
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class ChatActivity extends AppCompatActivity {

    private ProgressBar progressbar;

    private FirebaseListAdapter<ChatMessage> adapter;

    private UserItem user;

    private static final String CHANNEL_ID = "MessageNotification";

    private static final int SIGN_IN_REQUEST_CODE = 2812;
    private static final int PICK_PHOTO_REQUEST_CODE = 2182;
    private static final int messageLength = 1000;
    private static int notificationId = 2812;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        new FirebaseHandler();
        FirebaseDatabase.getInstance().getReference().keepSynced(true);

        progressbar = findViewById(R.id.progressBar);
        progressbar.setVisibility(View.INVISIBLE);

        FirebaseApp.getInstance();

        final Intent intentAction = getIntent();
        String action = intentAction.getAction();
        String type = intentAction.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                receivedSharedImageHandler(intentAction); // Handle single image being sent
            } else if ("text/plain".equals(type)) {
                receivedSharedTextHandler(intentAction); // Handle text being sent
            }
        } else {
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
                                .setTosAndPrivacyPolicyUrls("https://github.com/gamemaker1/Babble/blob/master/termsofservice.md", "https://github.com/gamemaker1/Babble/blob/master/privacy.md")
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
                                Log.d("ChatActivity", msg);
                            }
                        });

                //sendNotifications();

            } else {

                // User is already signed in.

                final Query userToAdd = FirebaseDatabase.getInstance()
                        .getReference().child("messages")
                        .orderByChild("readState")
                        .equalTo(false);

                userToAdd.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot messageToNotify : dataSnapshot.getChildren()) {

                            //sendNotifications();

                            messageToNotify.getValue(UserMessage.class).setReadState(true);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                    }
                });

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
                                Log.d("ChatActivity", msg);
                            }
                        });

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

                Intent intent = getIntent();
                final ArrayList<String> bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");

                if (bubbleInfo != null) {
                    setTitle(bubbleInfo.get(0) + " - Chat");
                }

                // Load chat room contents
                displayChatMessages();

                final Button sendButton = findViewById(R.id.fab);

                sendButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        final EditText input = findViewById(R.id.input);
                        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(messageLength)});

                        if (input.getText().toString().trim().length() > 0) {

                            Log.w("ChatActivity", "Bello - " + bubbleInfo);
                            if (bubbleInfo != null) {

                                final Query messageToSend = FirebaseDatabase.getInstance()
                                        .getReference().child("groups")
                                        .orderByChild("groupId")
                                        .equalTo(Long.parseLong(bubbleInfo.get(1)));

                                messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                                            // Read the input field and push a new instance
                                            // of ChatMessage to the Firebase database

                                            final String userMessage = input.getText().toString();

                                            // Clear the input and display a loading indicator
                                            input.setText("");
                                            progressbar.setVisibility(View.VISIBLE);

                                            FirebaseDatabase.getInstance()
                                                    .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                                    .push()
                                                    .setValue(new ChatMessage(userMessage,
                                                            FirebaseAuth.getInstance()
                                                                    .getCurrentUser()
                                                                    .getDisplayName(),
                                                            null)
                                                    );

                                            FirebaseDatabase.getInstance()
                                                    .getReference().child("messages")
                                                    .push()
                                                    .setValue(new UserMessage(userMessage,
                                                            FirebaseAuth.getInstance()
                                                                    .getCurrentUser()
                                                                    .getDisplayName(),
                                                            null,
                                                            bubbleInfo.get(0),
                                                            Long.valueOf(bubbleInfo.get(1)),
                                                            new Date().getTime(),
                                                            false)
                                                    );

                                            adapter.notifyDataSetChanged();

                                            progressbar.setVisibility(View.INVISIBLE);

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

                ImageButton photoPicker = findViewById(R.id.photoPickerButton);

                photoPicker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/jpeg");
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        startActivityForResult(Intent
                                        .createChooser(intent, "Complete action using"),
                                PICK_PHOTO_REQUEST_CODE);
                    }
                });

                Button todoButton = findViewById(R.id.todoButton);

                todoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Log.w("ChatActivity", "Info - " + bubbleInfo);
                        Intent intent = new Intent(ChatActivity.this, TodoActivity.class);
                        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                        startActivityForResult(intent, RESULT_OK);

                    }
                });
            }
        }
    }

    private void receivedSharedTextHandler(Intent intentAction) {
        String sharedText = intentAction.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            Intent intent = new Intent(ChatActivity.this, SharedTextHandler.class);
            intent.putExtra("textToShare", sharedText);
            startActivity(intent);
        }
    }

    private void receivedSharedImageHandler(Intent intentAction) {
        Uri imageUri = intentAction.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
            Intent intent = new Intent(ChatActivity.this, GroupQuestion.class);
            intent.putExtra("type", "image");
            intent.putExtra("imageUri", imageUri);
            startActivity(intent);
        }
    }



    private void displayChatMessages() {

        progressbar.setVisibility(View.VISIBLE);

        final ListView listOfMessages = findViewById(R.id.list_of_messages);

        Intent intent = getIntent();
        final ArrayList<String> bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");

        Log.w("ChatActivity", "Bello - " + bubbleInfo);
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

                        adapter = new FirebaseListAdapter<ChatMessage>(ChatActivity.this, ChatMessage.class,
                                R.layout.message, FirebaseDatabase.getInstance()
                                .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages").limitToLast(100)) {

                            @Override
                            protected void populateView(View v, ChatMessage model, int position) {
                                // Get references to the views of message.xml
                                ImageView photoImageView = v.findViewById(R.id.photoImageView);
                                TextView messageText = v.findViewById(R.id.message_text);
                                TextView messageUser = v.findViewById(R.id.message_user);
                                TextView messageTime = v.findViewById(R.id.message_time);

                                ChatMessage message = getItem(position);

                                boolean isPhoto = message.getPhotoUrl() != null;
                                if (isPhoto) {
                                    messageText.setVisibility(View.GONE);
                                    photoImageView.setVisibility(View.VISIBLE);
                                    Glide.with(photoImageView.getContext())
                                            .load(message.getPhotoUrl())
                                            .into(photoImageView);
                                } else {
                                    messageText.setVisibility(View.VISIBLE);
                                    photoImageView.setVisibility(View.GONE);
                                    messageText.setText(message.getMessageText());
                                }

                                // Set their users and timestamps
                                messageUser.setText(message.getMessageUser());
                                // Format the date before showing it
                                messageTime.setText(DateFormat.format("dd-MM-yyyy @ HH:mm",
                                        model.getMessageTime()));
                            }
                        };

                        listOfMessages.setAdapter(adapter);

                        listOfMessages.smoothScrollToPosition(adapter.getCount() - 1);

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

        progressbar.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PHOTO_REQUEST_CODE) {
            progressbar.setVisibility(ProgressBar.VISIBLE);
            if (data != null) {
                Uri selectedImageUri = data.getData();
                assert selectedImageUri != null;
                final StorageReference photoRef = FirebaseStorage.getInstance()
                        .getReferenceFromUrl("gs://babble-875f6.appspot.com")
                        .child("chat_photos")
                        .child(selectedImageUri.getLastPathSegment());
                UploadTask uploadTask = photoRef.putFile(selectedImageUri);
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        // Continue with the task to get the download URL
                        return photoRef.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            final Uri downloadUri = task.getResult();

                            Intent intent = getIntent();
                            final ArrayList<String> bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");

                            if (bubbleInfo != null) {
                                final long bubbleId = Long.parseLong(bubbleInfo.get(1));

                                final Query messageToSend = FirebaseDatabase.getInstance()
                                        .getReference().child("groups")
                                        .orderByChild("groupId")
                                        .equalTo(Long.parseLong(bubbleInfo.get(1)));

                                messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                                            // Read the input field and push a new instance
                                            // of ChatMessage to the Firebase database

                                            FirebaseDatabase.getInstance()
                                                    .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                                    .push()
                                                    .setValue(new ChatMessage(null,
                                                            FirebaseAuth.getInstance()
                                                                    .getCurrentUser()
                                                                    .getDisplayName(),
                                                            downloadUri.toString())
                                                    );

                                            adapter.notifyDataSetChanged();

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

                            progressbar.setVisibility(ProgressBar.INVISIBLE);

                            //sendNotifications();

                        } else {
                            // Handle Failures
                            Toast.makeText(ChatActivity.this, "Sorry, the image could not be sent. Try again a bit later.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                // cancelled photo picker intent
                progressbar.setVisibility(ProgressBar.INVISIBLE);
            }
        } else if(requestCode == SIGN_IN_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(this,
                        "You're successfully signed in. Welcome! :)",
                        Toast.LENGTH_LONG)
                        .show();
                displayChatMessages();
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
                            Toast.makeText(ChatActivity.this,
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
                                        .delete(ChatActivity.this)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(ChatActivity.this,
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

            Intent intent = new Intent(ChatActivity.this, AddFriend.class);
            intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
            startActivity(intent);

        } else if (item.getItemId() == R.id.legal) {

            Intent intent = new Intent(ChatActivity.this, LegalActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == R.id.menu_group_info) {

            Intent intentToReceive = getIntent();
            ArrayList<String> bubbleInfo = new ArrayList<>(intentToReceive.getStringArrayListExtra("bubbleInfo"));

            Intent intent = new Intent(ChatActivity.this, GroupInfoActivity.class);
            intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
            startActivity(intent);

        /*} else if (item.getItemId() == R.id.menu_delete_group) {

            Intent intent2 = getIntent();
            final ArrayList<String> bubbleInfo = intent2.getStringArrayListExtra("bubbleInfo");

            final ArrayList<String> groupArrayForLeaving = new ArrayList<>();

            final Query messageToSend = FirebaseDatabase.getInstance()
                    .getReference().child("groups")
                    .orderByChild("groupId")
                    .equalTo(Long.parseLong(bubbleInfo.get(1)));

            messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot emailToRemove : dataSnapshot.getChildren()) {
                        groupArrayForLeaving.addAll(emailToRemove.getValue(GroupItem.class).getPeopleInGroup());
                    }

                    for (String s : groupArrayForLeaving) {
                        if (s.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
                            groupArrayForLeaving.remove(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                        }

                    }


                    Log.w("IN 'ERE", "DELETED 1 - " + groupArrayForLeaving);

                    final Query messageToSend = FirebaseDatabase.getInstance()
                            .getReference().child("groups")
                            .orderByChild("groupId")
                            .equalTo(Long.parseLong(bubbleInfo.get(1)));

                    messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot groupArrayToSet : dataSnapshot.getChildren()) {
                                Log.w("IN 'ERE", "EXISTING  - " + groupArrayForLeaving);

                                groupArrayToSet.getValue(GroupItem.class).setPeopleInGroup(groupArrayForLeaving);

                                Log.w("IN 'ERE", "DELETED 2 - " + groupArrayToSet.getValue(GroupItem.class).getPeopleInGroup());
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

            Intent intent = new Intent(ChatActivity.this, GroupsActivity.class);
            startActivity(intent);
        */
        } else if (item.getItemId() == android.R.id.home) {
            Intent backIntent = new Intent(ChatActivity.this, GroupsActivity.class);
            startActivity(backIntent);
        }
        return true;
    }
}