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

public class OverlayService extends Service {
    private static final String TAG = "OverlayService";
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "▶️ Starting overlay service");
        createCalmOverlay();
        return START_NOT_STICKY; // Don't restart automatically
    }

    private void createCalmOverlay() {
        if (overlayView != null) {
            return; // Already created
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, // Very small size - almost invisible
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        try {
            overlayView = new View(this);
            overlayView.setBackgroundColor(0x00000000); // Completely transparent

            windowManager.addView(overlayView, params);
            Log.d(TAG, "✅ Calm overlay created");

            // Auto-remove after short time
            new android.os.Handler().postDelayed(() -> {
                removeOverlay();
            }, 3000); // Only 3 seconds

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating overlay: " + e.getMessage());
        }
    }

    private void removeOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
                overlayView = null;
                Log.d(TAG, "✅ Overlay removed");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error removing overlay: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlay();
        Log.d(TAG, "🔚 Overlay service destroyed");
    }
}