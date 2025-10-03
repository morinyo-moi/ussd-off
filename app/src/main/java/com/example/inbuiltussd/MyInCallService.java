package com.example.inbuiltussd;

import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;

public class MyInCallService extends InCallService {

    private static final String TAG = "MyInCallService";

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "Call added: " + call);

        // You can register a callback here to listen for call events
        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                super.onStateChanged(call, state);
                Log.d(TAG, "Call state changed: " + state);
            }

            @Override
            public void onDetailsChanged(Call call, Call.Details details) {
                super.onDetailsChanged(call, details);
                Log.d(TAG, "Call details changed: " + details);
            }
        });
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "Call removed: " + call);
    }
}
