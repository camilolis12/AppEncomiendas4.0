package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RastrearActivity extends AppCompatActivity {

    EditText edtRadicado;
    Button btnBuscar, btnVerMapa;
    TextView txtResultado;

    private static final String CHANNEL_ID = "rastreo_paquetes";

    // Guardamos lat/lng consultados para pasarlos al mapa
    double latitud = 0, longitud = 0;
    String radicadoEncontrado = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rastrear);

        edtRadicado = findViewById(R.id.edtRadicado);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnVerMapa = findViewById(R.id.btnVerMapa);
        txtResultado = findViewById(R.id.txtResultado);

        crearCanalNotificaciones();

        btnBuscar.setOnClickListener(v -> buscarPaquete());

        btnVerMapa.setOnClickListener(v -> {
            if (radicadoEncontrado.isEmpty()) {
                Toast.makeText(this, "Primero busque un paquete válido", Toast.LENGTH_SHORT).show();
            } else {
                double lat = latitud;
                double lng = longitud;
                if (lat == 0 && lng == 0) {
                    lat = 4.7110;
                    lng = -74.0721;
                    Toast.makeText(this, "No hay coordenadas registradas, mostrando Bogotá", Toast.LENGTH_SHORT).show();
                }
                // Abrir Google Maps con la ubicación
                String uri = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(Paquete " + radicadoEncontrado + ")";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    // Si falla, abrir en navegador
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(browserIntent);
                }
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
