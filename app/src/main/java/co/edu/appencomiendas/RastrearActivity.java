package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class RastrearActivity extends AppCompatActivity {

    EditText edtRadicado;
    Button btnBuscar, btnVerMapa, btnReportarUbicacion;
    TextView txtResultado;

    private static final String CHANNEL_ID = "rastreo_paquetes";
    private static final int REQ_LOCATION = 202;

    // Guardamos lat/lng consultados para pasarlos al mapa
    double latitud = 0, longitud = 0;
    String radicadoEncontrado = "";

    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rastrear);

        edtRadicado = findViewById(R.id.edtRadicado);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnVerMapa = findViewById(R.id.btnVerMapa);
        btnReportarUbicacion = findViewById(R.id.btnReportarUbicacion);
        txtResultado = findViewById(R.id.txtResultado);

        crearCanalNotificaciones();

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        btnBuscar.setOnClickListener(v -> buscarPaquete());

        btnVerMapa.setOnClickListener(v -> {
            if (radicadoEncontrado.isEmpty()) {
                Toast.makeText(this, "Primero busque un paquete válido", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, MapaActivity.class);
                intent.putExtra("radicado", radicadoEncontrado);
                intent.putExtra("latitud", latitud);
                intent.putExtra("longitud", longitud);
                startActivity(intent);
            }
        });

        btnReportarUbicacion.setOnClickListener(v -> {
            if (radicadoEncontrado.isEmpty()) {
                Toast.makeText(this, "Busque un paquete antes de reportar ubicación", Toast.LENGTH_SHORT).show();
            } else {
                solicitarYGuardarUbicacion();
            }
        });
    }

    private void buscarPaquete() {
        String radicado = edtRadicado.getText().toString().trim();

        if (radicado.isEmpty()) {
            Toast.makeText(this, "Ingrese un número de radicado", Toast.LENGTH_SHORT).show();
            return;
        }

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT remitenteNombre, destinatarioNombre, tamano, peso, longitud, estado, latitud, longitudGeo, express, tarifa " +
                "FROM " + DBHelper.TABLE_PAQUETES + " WHERE radicado = ?", new String[]{radicado});

        if (cursor.moveToFirst()) {
            String remitente = cursor.getString(0);
            String destinatario = cursor.getString(1);
            String tamano = cursor.getString(2);
            String peso = cursor.getString(3);
            String longitudStr = cursor.getString(4);
            String estado = cursor.getString(5);
            boolean isExpress = cursor.getInt(8) == 1;
            String tarifaDb = cursor.getString(9);

            // Calcular tarifa base según tamaño
            int tarifaBase = 0;
            String tamanoLower = tamano.trim().toLowerCase();
            if ("pequeño".equals(tamanoLower)) {
                tarifaBase = 15000;
            } else if ("mediano".equals(tamanoLower)) {
                tarifaBase = 30000;
            } else if ("grande".equals(tamanoLower)) {
                tarifaBase = 60000;
            }

            // Sumar tarifa Express si aplica
            int tarifaTotal = tarifaBase + (isExpress ? 25000 : 0);

            // Lectura segura de coordenadas (pueden ser NULL)
            if (!cursor.isNull(6)) {
                latitud = cursor.getDouble(6);
            } else {
                latitud = 0;
            }
            if (!cursor.isNull(7)) {
                longitud = cursor.getDouble(7); // ahora longitud contiene la longitud GPS (longitudGeo)
            } else {
                longitud = 0;
            }

            radicadoEncontrado = radicado;

            String info = "📦 Radicado: " + radicado + "\n" +
                    "Remitente: " + remitente + "\n" +
                    "Destinatario: " + destinatario + "\n" +
                    "Tamaño: " + tamano + "\n" +
                    "Peso: " + peso + " kg\n" +
                    "Longitud: " + longitudStr + " cm\n" +
                    "Estado: " + estado + "\n" +
                    "Tipo de envío: " + (isExpress ? "Express/Premium ⚡" : "Normal") + "\n" +
                    "Tarifa base: $" + String.format("%,d", tarifaBase) + " COP\n" +
                    (isExpress ? "Cargo Express: $25,000 COP\n" : "") +
                    "Total: $" + String.format("%,d", tarifaTotal) + " COP" +
                    (latitud != 0 && longitud != 0 ? "\nUbicación disponible ✅" : "\nUbicación no registrada ❌");

            txtResultado.setText(info);

            mostrarNotificacion(radicado, estado);

        } else {
            txtResultado.setText("❌ Paquete no encontrado");
            radicadoEncontrado = "";
            latitud = 0;
            longitud = 0;
        }

        cursor.close();
        db.close();
    }

    private void solicitarYGuardarUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }

        CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedClient.getCurrentLocation(req, null).addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(this, "No se pudo obtener ubicación actual", Toast.LENGTH_SHORT).show();
                return;
            }

            latitud = location.getLatitude();
            longitud = location.getLongitude();

            DBHelper helper = new DBHelper(this);
            SQLiteDatabase db = helper.getWritableDatabase();

            // Actualizar ubicación más reciente del paquete
            ContentValues cv = new ContentValues();
            cv.put("latitud", latitud);
            cv.put("longitudGeo", longitud);
            db.update(DBHelper.TABLE_PAQUETES, cv, "radicado = ?", new String[]{radicadoEncontrado});

            // Registrar punto en historial de tracking
            ContentValues punto = new ContentValues();
            punto.put("radicado", radicadoEncontrado);
            punto.put("latitud", latitud);
            punto.put("longitud", longitud);
            punto.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            db.insert("tracking_points", null, punto);

            db.close();

            Toast.makeText(this, "Ubicación reportada para " + radicadoEncontrado, Toast.LENGTH_SHORT).show();

            if (!txtResultado.getText().toString().contains("Último punto")) {
                txtResultado.append("\n\nÚltimo punto: " + latitud + ", " + longitud);
            } else {
                txtResultado.setText(txtResultado.getText().toString().replaceAll("Último punto: .+", "Último punto: " + latitud + ", " + longitud));
            }
        });
    }


    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Canal Rastreo";
            String description = "Notificaciones de rastreo de paquetes";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                solicitarYGuardarUbicacion();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mostrarNotificacion(String radicado, String estado) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Estado del paquete " + radicado)
                .setContentText("El estado actual es: " + estado)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
