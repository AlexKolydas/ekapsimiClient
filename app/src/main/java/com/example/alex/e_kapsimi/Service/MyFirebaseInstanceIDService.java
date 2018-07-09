package com.example.alex.e_kapsimi.Service;

import android.util.Log;

import com.example.alex.e_kapsimi.Common.Common;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.example.Model.Token;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService{

    //gia na paro to token apo firebase to opoio tto dinei tin proti fora otan trexo to app
    //opote prepeii na kano uninstall to app apo to emulator kai ksana install    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String refreshedToken=FirebaseInstanceId.getInstance().getToken();
        if(Common.current_user!=null)
            updateTokenToFirebase(refreshedToken);

    }

    private void updateTokenToFirebase(String refreshedToken) {
        FirebaseDatabase db=FirebaseDatabase.getInstance();
        DatabaseReference tokens=db.getReference("Tokens");
        Token token=new Token(refreshedToken,false);
        tokens.child(Common.current_user.getPhone()).setValue(token);
    }
}
