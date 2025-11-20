package co.edu.appencomiendas;

import android.os.Bundle;
import android.widget.TextView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.Toast;

public class DetallePaqueteActivity extends AppCompatActivity {

    TextView txtDetalle;
    Button btnCalificar, btnCopiarRadicado, btnStartTracking, btnStopTracking;
    int idPaquete;
    String radicadoActual = "";
    double destinoLat = 0, destinoLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_paquete);

        txtDetalle = findViewById(R.id.txtDetallePaquete);
        btnCalificar = findViewById(R.id.btnCalificarPaquete);
        btnCopiarRadicado = findViewById(R.id.btnCopiarRadicado);
        btnStartTracking = findViewById(R.id.btnStartTracking);
        btnStopTracking = findViewById(R.id.btnStopTracking);

        idPaquete = getIntent().getIntExtra("idPaquete", -1);

        if (idPaquete != -1) {
            mostrarDetalle();
        } else {
            txtDetalle.setText("No se encontró información del paquete.");
            btnCalificar.setEnabled(false);
            btnCopiarRadicado.setEnabled(false);
        }

        btnCalificar.setOnClickListener(v -> {
            Intent intent = new Intent(DetallePaqueteActivity.this, CalificarActivity.class);
            intent.putExtra("idPaquete", idPaquete);
            startActivity(intent);
        });

        btnCopiarRadicado.setOnClickListener(v -> {
            if (!radicadoActual.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Radicado", radicadoActual);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Radicado copiado al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });

        btnStartTracking.setOnClickListener(v -> {
            if (radicadoActual.isEmpty()) { Toast.makeText(this,"No hay radicado",Toast.LENGTH_SHORT).show(); return; }
            Intent i = new Intent(this, LocationForegroundService.class);
            i.setAction(LocationForegroundService.ACTION_START);
            i.putExtra(LocationForegroundService.EXTRA_RADICADO, radicadoActual);
            // pasar coordenadas destino si existen en la BD
            SQLiteDatabase db = new DBHelper(this).getReadableDatabase();
            Cursor cur = db.rawQuery("SELECT latitud, longitudGeo FROM " + DBHelper.TABLE_PAQUETES + " WHERE radicado = ?", new String[]{radicadoActual});
            if (cur.moveToFirst()) {
                if (!cur.isNull(0)) destinoLat = cur.getDouble(0);
                if (!cur.isNull(1)) destinoLng = cur.getDouble(1);
            }
            cur.close(); db.close();
            i.putExtra(LocationForegroundService.EXTRA_DEST_LAT, destinoLat);
            i.putExtra(LocationForegroundService.EXTRA_DEST_LNG, destinoLng);
            startService(i);
            Toast.makeText(this, "Tracking iniciado", Toast.LENGTH_SHORT).show();
        });

        btnStopTracking.setOnClickListener(v -> {
            Intent i = new Intent(this, LocationForegroundService.class);
            i.setAction(LocationForegroundService.ACTION_STOP);
            startService(i);
            Toast.makeText(this, "Detenido tracking", Toast.LENGTH_SHORT).show();
        });
    }

    private void mostrarDetalle() {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT radicado, remitenteNombre, remitenteTelefono, remitenteDireccion, destinatarioNombre, destinatarioTelefono, destinatarioDireccion, tamano, peso, longitud, metodoPago, estado, calificacion, express, tarifa FROM "
                + DBHelper.TABLE_PAQUETES + " WHERE id = ?", new String[]{String.valueOf(idPaquete)});

        if (cursor.moveToFirst()) {
            radicadoActual = cursor.getString(0);
            boolean isExpress = cursor.getInt(13) == 1;
            String tarifa = cursor.getString(14);

            String info =
                    "Radicado: " + radicadoActual + "\n" +
                    "Remitente: " + cursor.getString(1) + "\n" +
                    "Teléfono Remitente: " + cursor.getString(2) + "\n" +
                    "Dirección Remitente: " + cursor.getString(3) + "\n" +
                    "Destinatario: " + cursor.getString(4) + "\n" +
                    "Teléfono Destinatario: " + cursor.getString(5) + "\n" +
                    "Dirección Destinatario: " + cursor.getString(6) + "\n" +
                    "Tamaño: " + cursor.getString(7) + "\n" +
                    "Peso: " + cursor.getString(8) + " kg\n" +
                    "Longitud: " + cursor.getString(9) + " cm\n" +
                    "Método de pago: " + cursor.getString(10) + "\n" +
                    "Estado: " + cursor.getString(11) + "\n" +
                    "Calificación: " + cursor.getInt(12) + "\n" +
                    "Tipo de envío: " + (isExpress ? "Express/Premium ⚡" : "Normal") + "\n" +
                    "Tarifa: $" + tarifa + " COP";

            txtDetalle.setText(info);
        } else {
            txtDetalle.setText("No se encontró información del paquete.");
            btnCalificar.setEnabled(false);
            btnCopiarRadicado.setEnabled(false);
        }

        cursor.close();
        db.close();
    }
}
