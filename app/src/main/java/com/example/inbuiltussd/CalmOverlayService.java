package com.example.inbuiltussd;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class CalmOverlayService extends Service {
    private static final String TAG = "CalmOverlay";
    private View overlayView;
    private WindowManager windowManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 Creating calm overlay service");
        createStableOverlay();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "▶️ Overlay service started");
        return START_STICKY;
    }

    private void createStableOverlay() {
        if (overlayView != null) {
            Log.d(TAG, "⚠️ Overlay already exists");
            return;
        }

        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    1, 1, // Minimal size
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 0;
            params.alpha = 0.01f; // Almost transparent but stable

            overlayView = new View(this);
            overlayView.setBackgroundColor(0x01000000); // Nearly transparent

            windowManager.addView(overlayView, params);
            Log.d(TAG, "✅ Stable overlay created");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating overlay: " + e.getMessage());
        }
    }

    private void removeOverlaySafe() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
                overlayView = null;
                Log.d(TAG, "✅ Overlay safely removed");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error removing overlay: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlaySafe();
        Log.d(TAG, "🔚 Calm overlay service destroyed");
    }
}