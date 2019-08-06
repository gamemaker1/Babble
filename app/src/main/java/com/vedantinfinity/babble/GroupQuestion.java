package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class GroupQuestion extends AppCompatActivity {

    private ProgressBar progressBar;
    private ListView listOfGroups;

    private ArrayAdapter<String> adapter;
    private ArrayList<String> userGroupsArray;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_question);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        listOfGroups = findViewById(R.id.list_of_groups);

        displayUserGroupsToSendTo();

        listOfGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                final Query messageToSend = FirebaseDatabase.getInstance()
                        .getReference().child("groups")
                        .orderByChild("groupTitle")
                        .equalTo(adapter.getItem(position));

                messageToSend.addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (final DataSnapshot childHeaders : dataSnapshot.getChildren()) {

                            Intent receivedIntent = getIntent();
                            String type = receivedIntent.getStringExtra("type");

                            if (type.equals("message")) {

                                final String textToShare = receivedIntent.getStringExtra("textToShare");

                                FirebaseDatabase.getInstance()
                                        .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("messages")
                                        .push()
                                        .setValue(new ChatMessage(textToShare,
                                                FirebaseAuth.getInstance()
                                                        .getCurrentUser()
                                                        .getDisplayName(),
                                                null)
                                        );

                                final ArrayList<String> bubbleInfo = new ArrayList<>();

                                final Query bubbleQuery = FirebaseDatabase.getInstance()
                                        .getReference().child("groups")
                                        .orderByChild("groupTitle")
                                        .equalTo(userGroupsArray.get(position));

                                bubbleQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                                            Log.w("GroupsActivity", "Data Now - " + data);
                                            String bubbleNameForInfo = Objects.requireNonNull(data.getValue(GroupItem.class)).getGroupTitle();
                                            long bubbleIdForInfo = Objects.requireNonNull(data.getValue(GroupItem.class)).getGroupId();

                                            bubbleInfo.add(bubbleNameForInfo);
                                            bubbleInfo.add(String.valueOf(bubbleIdForInfo));

                                            Log.w("GroupsActivity", "Info Now - " + bubbleInfo);

                                        }

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("messages")
                                                .push()
                                                .setValue(new UserMessage(textToShare,
                                                        FirebaseAuth.getInstance()
                                                                .getCurrentUser()
                                                                .getDisplayName(),
                                                        null,
                                                        bubbleInfo.get(0),
                                                        Long.parseLong(bubbleInfo.get(1)),
                                                        new Date().getTime(),
                                                        false)
                                                );

                                        Log.w("GroupsActivity", "Info 2 Now - " + bubbleInfo);
                                        Intent intent = new Intent(GroupQuestion.this, ChatActivity.class);
                                        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                                        startActivity(intent);

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                                    }
                                });

                            } else if (type.equals("todo")) {

                                final String textToShare = receivedIntent.getStringExtra("textToShare");

                                FirebaseDatabase.getInstance()
                                        .getReference().child("groups").child(Objects.requireNonNull(childHeaders.getKey())).child("todos")
                                        .push()
                                        .setValue(new TodoItem(textToShare,
                                                null, UUID.randomUUID().getLeastSignificantBits())
                                        );

                                final ArrayList<String> bubbleInfo = new ArrayList<>();

                                final Query bubbleQuery = FirebaseDatabase.getInstance()
                                        .getReference().child("groups")
                                        .orderByChild("groupTitle")
                                        .equalTo(userGroupsArray.get(position));

                                bubbleQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                                            Log.w("GroupsActivity", "Data Now - " + data);
                                            String bubbleNameForInfo = Objects.requireNonNull(data.getValue(GroupItem.class)).getGroupTitle();
                                            long bubbleIdForInfo = Objects.requireNonNull(data.getValue(GroupItem.class)).getGroupId();

                                            bubbleInfo.add(bubbleNameForInfo);
                                            bubbleInfo.add(String.valueOf(bubbleIdForInfo));

                                            Log.w("GroupsActivity", "Info Now - " + bubbleInfo);

                                        }

                                        FirebaseDatabase.getInstance()
                                                .getReference().child("messages")
                                                .push()
                                                .setValue(new UserMessage("I have added the task " + textToShare + " !",
                                                        FirebaseAuth.getInstance()
                                                                .getCurrentUser()
                                                                .getDisplayName(),
                                                        null,
                                                        bubbleInfo.get(0),
                                                        Long.parseLong(bubbleInfo.get(1)),
                                                        new Date().getTime(),
                                                        false)
                                                );

                                        Log.w("GroupsActivity", "Info 2 Now - " + bubbleInfo);
                                        Intent intent = new Intent(GroupQuestion.this, TodoActivity.class);
                                        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                                        startActivity(intent);

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                                    }
                                });

                            } else if (type.equals("image")) {

                                if (receivedIntent.getExtras() != null) {

                                    final String imageUri = receivedIntent.getExtras().getString("imageUri");
                                    final Uri selectedImageUri = Uri.parse(imageUri);
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

                                                                final ArrayList<String> bubbleInfo = new ArrayList<>();

                                                                final Query bubbleQuery = FirebaseDatabase.getInstance()
                                                                        .getReference().child("groups")
                                                                        .orderByChild("groupTitle")
                                                                        .equalTo(userGroupsArray.get(position));

                                                                bubbleQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                                                                            Log.w("GroupsActivity", "Data Now - " + data);
                                                                            String bubbleNameForInfo = Objects.requireNonNull(data.getValue(GroupItem.class)).getGroupTitle();
                                                                            long bubbleIdForInfo = Objects.requireNonNull(data.getValue(GroupItem.class)).getGroupId();

                                                                            bubbleInfo.add(bubbleNameForInfo);
                                                                            bubbleInfo.add(String.valueOf(bubbleIdForInfo));

                                                                            Log.w("GroupsActivity", "Info Now - " + bubbleInfo);

                                                                        }

                                                                        FirebaseDatabase.getInstance()
                                                                                .getReference().child("messages")
                                                                                .push()
                                                                                .setValue(new UserMessage(null,
                                                                                        FirebaseAuth.getInstance()
                                                                                                .getCurrentUser()
                                                                                                .getDisplayName(),
                                                                                        downloadUri.toString(),
                                                                                        bubbleInfo.get(0),
                                                                                        Long.parseLong(bubbleInfo.get(1)),
                                                                                        new Date().getTime(),
                                                                                        false)
                                                                                );

                                                                        Log.w("GroupsActivity", "Info 2 Now - " + bubbleInfo);
                                                                        Intent intent = new Intent(GroupQuestion.this, ChatActivity.class);
                                                                        intent.putStringArrayListExtra("bubbleInfo", bubbleInfo);
                                                                        startActivity(intent);

                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                                                                    }
                                                                });

                                                                adapter.notifyDataSetChanged();

                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                                            Log.e("ItemDetail", "onCancelled", databaseError.toException());
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("ItemDetail", "onCancelled", databaseError.toException());
                    }
                });

            }

        });

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void displayUserGroupsToSendTo() {

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

                    adapter = new ArrayAdapter<>(GroupQuestion.this, R.layout.group_item,
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

}
