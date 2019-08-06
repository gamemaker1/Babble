package com.vedantinfinity.babble;

/*
Copyright (C) 2019 Vedant K

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see https://www.gnu.org/licenses/.

*/

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SharedTextHandler extends AppCompatActivity {

    private Button addAsTodoButton;
    private Button sendAsMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_text_handler);

        addAsTodoButton = findViewById(R.id.add_as_todo);
        sendAsMessage = findViewById(R.id.add_as_message);

        Intent receivedIntent = getIntent();
        final String textToShare = receivedIntent.getStringExtra("textToShare");

        addAsTodoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SharedTextHandler.this, GroupQuestion.class);
                intent.putExtra("type", "todo");
                intent.putExtra("textToShare", textToShare);
                startActivity(intent);
            }
        });


        sendAsMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SharedTextHandler.this, GroupQuestion.class);
                intent.putExtra("type", "message");
                intent.putExtra("textToShare", textToShare);
                startActivity(intent);
            }
        });

    }
}
