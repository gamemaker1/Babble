package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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

public class GroupInfoActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private ArrayList<String> userInfo;
    private ArrayList<String> bubbleInfo;

    private ListView listOfUsers;
    private ProgressBar progressBar;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        Intent intent = getIntent();
        bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");

        listOfUsers = findViewById(R.id.list_of_users);
        progressBar = findViewById(R.id.progressBar);

        setTitle(bubbleInfo.get(0));

        displayGroupUsers();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void displayGroupUsers() {

        Intent intent = getIntent();
        bubbleInfo = intent.getStringArrayListExtra("bubbleInfo");

        if (bubbleInfo != null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                progressBar.setVisibility(View.VISIBLE);

                userInfo = new ArrayList<>();

                final Query groupsQuery = FirebaseDatabase.getInstance()
                        .getReference().child("groups")
                        .orderByChild("groupTitle")
                        .equalTo(bubbleInfo.get(0));

                groupsQuery.addValueEventListener(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (final DataSnapshot groupHeader : dataSnapshot.getChildren()) {
                            final Query groupsQuery = FirebaseDatabase.getInstance()
                                    .getReference().child("users")
                                    .orderByChild("userEmailAddress")
                                    ;

                            groupsQuery.addValueEventListener(new ValueEventListener() {
                                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                                @Override
                                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {

                                    for (DataSnapshot user : dataSnapshot.getChildren()) {
                                        String userEmail = user.getValue(UserItem.class).getUserEmailAddress();
                                        String userDisplayName = user.getValue(UserItem.class).getUserDisplayName();
                                        String userInfoString = userDisplayName + "\n" + userEmail;
                                        Log.w("GroupInfoActivity", "userInfo Now - " + userInfoString);
                                        Log.w("GroupInfoActivity", "Bool " + groupHeader.getValue(GroupItem.class).getPeopleInGroup().contains(userEmail));
                                        if (groupHeader.getValue(GroupItem.class).getPeopleInGroup().contains(userEmail)) {
                                            userInfo.add(userInfoString);
                                        }
                                    }

                                    adapter = new ArrayAdapter<>(GroupInfoActivity.this, R.layout.group_info_item,
                                            userInfo);

                                    adapter.notifyDataSetChanged();

                                    listOfUsers.setAdapter(adapter);

                                    progressBar.setVisibility(View.INVISIBLE);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.e("GroupInfoActivity", "onCancelled", databaseError.toException());
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("GroupInfoActivity", "onCancelled", databaseError.toException());
                    }
                });

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
                            Toast.makeText(GroupInfoActivity.this,
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
                                        .delete(GroupInfoActivity.this)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(GroupInfoActivity.this,
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

            Intent intent = new Intent(GroupInfoActivity.this, LegalActivity.class);
            startActivity(intent);

        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }
}

