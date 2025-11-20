package co.edu.appencomiendas;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Build;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.*;
import android.location.Location;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationForegroundService extends Service {

    public static final String ACTION_START = "ACTION_START_TRACKING";
    public static final String ACTION_STOP = "ACTION_STOP_TRACKING";
    public static final String EXTRA_RADICADO = "radicado";
    public static final String EXTRA_DEST_LAT = "dest_lat";
    public static final String EXTRA_DEST_LNG = "dest_lng";

    private FusedLocationProviderClient fusedClient;
    private LocationCallback callback;
    private String radicado;
    private double destLat = 0, destLng = 0;
    private static final String CHANNEL_ID = "tracking_channel";
    private static final int NOTIF_ID = 9999;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        // Crear canal de notificación si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Tracking", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Notificaciones del tracking de encomiendas");
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action) || action == null) {
            radicado = intent.getStringExtra(EXTRA_RADICADO);
            destLat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0);
            destLng = intent.getDoubleExtra(EXTRA_DEST_LNG, 0);
            startForeground(NOTIF_ID, buildNotification("Rastreando " + radicado));
            startLocationUpdates();
            return START_STICKY;
        } else if (ACTION_STOP.equals(action)) {
            stopLocationUpdates();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private Notification buildNotification(String text) {
        Intent stopIntent = new Intent(this, LocationForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pStop = PendingIntent.getService(this, 0, stopIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Tracking en curso")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(new NotificationCompat.Action(R.drawable.ic_launcher_foreground, "Detener", pStop))
                .setOngoing(true);
        return builder.build();
    }

    private void startLocationUpdates() {
        try {
            LocationRequest req = LocationRequest.create();
            req.setInterval(10_000);
            req.setFastestInterval(5_000);
            req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            callback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult result) {
                    for (Location loc : result.getLocations()) {
                        savePoint(loc);
                        checkArrival(loc);
                    }
                }
            };
            fusedClient.requestLocationUpdates(req, callback, getMainLooper());
        } catch (SecurityException se) {
            Log.e("LocationService", "permiso denegado", se);
        }
    }

    private void stopLocationUpdates() {
        if (callback != null) fusedClient.removeLocationUpdates(callback);
    }

    private void savePoint(Location loc) {
        try {
            DBHelper dbHelper = new DBHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("radicado", radicado);
            cv.put("latitud", loc.getLatitude());
            cv.put("longitud", loc.getLongitude());
            cv.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            db.insert("tracking_points", null, cv);
            db.close();
        } catch (Exception e) {
            Log.e("LocationService", "savePoint error", e);
        }
    }

    private void checkArrival(Location loc) {
        if (destLat == 0 && destLng == 0) return;
        float[] results = new float[1];
        Location.distanceBetween(loc.getLatitude(), loc.getLongitude(), destLat, destLng, results);
        if (results[0] <= 50) { // umbral 50 metros
            // notificar llegada
            NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Encomienda llegó")
                    .setContentText("Radicado " + radicado + " ha llegado al destino.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), b.build());
            // opcional: actualizar estado en DB
            try {
                DBHelper dbHelper = new DBHelper(this);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("estado", "Entregado");
                db.update(DBHelper.TABLE_PAQUETES, cv, "radicado = ?", new String[]{radicado});
                db.close();
            } catch (Exception e) { }
            // detener tracking automáticamente
            stopLocationUpdates();
            stopForeground(true);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
