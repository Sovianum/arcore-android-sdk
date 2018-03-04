package com.google.ar.core.examples.java.helloar.auth;


import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.ar.core.examples.java.helloar.MainActivity;
import com.google.ar.core.examples.java.helloar.R;

public class AuthFragment extends Fragment {
    protected void onGettingToken(String token) {
        if (getActivity() == null) {
            return;
        }

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(getActivity()).edit();

        editor.putString(getResources().getString(R.string.json_web_token), token);
        editor.apply();

        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
    }

    //only for debug!!!
    protected void getStubsToken() {
        String tokenStub = "tokeStub";
        onGettingToken(tokenStub);
    }
 }
