package com.vedantinfinity.babble;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHandler extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

    }
}
