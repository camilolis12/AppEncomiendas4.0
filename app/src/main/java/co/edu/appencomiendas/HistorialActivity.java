package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.app.AlertDialog;
import android.net.Uri;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class HistorialActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private ListView listViewHistorial;
    private ArrayList<String> radicados;
    private ArrayAdapter<String> adapter;
    private String usuarioActual;
    private Set<String> radicadosSeleccionados;
    private Button btnAgregarRuta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        usuarioActual = getIntent().getStringExtra("usuario");
        dbHelper = new DBHelper(this);
        listViewHistorial = findViewById(R.id.listViewHistorial);
        btnAgregarRuta = findViewById(R.id.btnAgregarRuta);
        radicadosSeleccionados = new HashSet<>();

        cargarHistorial();

        listViewHistorial.setOnItemClickListener((parent, view, position, id) -> {
            if (position < radicados.size()) {
                String radicado = radicados.get(position);
                if (radicadosSeleccionados.contains(radicado)) {
                    radicadosSeleccionados.remove(radicado);
                    Toast.makeText(this, "Encomienda deseleccionada: " + radicado, Toast.LENGTH_SHORT).show();
                } else {
                    radicadosSeleccionados.add(radicado);
                    Toast.makeText(this, "Encomienda seleccionada: " + radicado, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAgregarRuta.setOnClickListener(v -> agregarParadasEnRuta());
    }

    private void cargarHistorial() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        radicados.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            if (usuarioActual == null || usuarioActual.isEmpty()) {
                adapter.add("Error: usuario no válido");
                listViewHistorial.setAdapter(adapter);
                db.close();
                return;
            }

            // Obtener nombre completo del usuario actual
            String nombreCompleto = "";
            Cursor cNombre = db.rawQuery("SELECT nombreCompleto FROM " + DBHelper.TABLE_USUARIOS + " WHERE usuario = ?", new String[]{usuarioActual});
            if (cNombre.moveToFirst()) {
                nombreCompleto = cNombre.getString(0);
            }
            cNombre.close();

            if (nombreCompleto.isEmpty()) {
                adapter.add("No se encontró el perfil del usuario");
                listViewHistorial.setAdapter(adapter);
                db.close();
                return;
            }

            // Mostrar solo las encomiendas del usuario actual
            Cursor c = db.rawQuery(
                "SELECT radicado, destinatarioNombre, estado, tarifa FROM " + DBHelper.TABLE_PAQUETES +
                " WHERE remitenteNombre = ? ORDER BY id DESC",
                new String[]{nombreCompleto});

            if (c.getCount() > 0) {
                while (c.moveToNext()) {
                    String radicado = c.getString(0);
                    String destinatario = c.getString(1);
                    String estado = c.getString(2);
                    String tarifa = c.getString(3);

                    String item = "📦 " + radicado + "\n" + destinatario + " | " + estado + " | " + tarifa;
                    adapter.add(item);
                    radicados.add(radicado);
                }
            } else {
                adapter.add("Sin encomiendas registradas");
            }
            c.close();
            listViewHistorial.setAdapter(adapter);
        } catch (Exception e) {
            adapter.clear();
            adapter.add("Error al cargar historial: " + e.getMessage());
            listViewHistorial.setAdapter(adapter);
        } finally {
            db.close();
        }
    }

    private void mostrarOpciones(String radicado) {
        new AlertDialog.Builder(this)
                .setTitle("Opciones")
                .setItems(new String[]{"Ver detalles", "Calificar"}, (dialog, which) -> {
                    if (which == 0) {
                        verDetalles(radicado);
                    } else if (which == 1) {
                        mostrarCalificacion(radicado);
                    }
                })
                .show();
    }

    private void verDetalles(String radicado) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT remitenteNombre, remitenteTelefono, remitenteDireccion, destinatarioNombre, destinatarioTelefono, destinatarioDireccion, tamano, peso, longitud, metodoPago, estado, tarifa FROM " +
            DBHelper.TABLE_PAQUETES + " WHERE radicado = ?", new String[]{radicado});

        if (c.moveToFirst()) {
            String detalles = "📋 DETALLES\n\n" +
                    "Radicado: " + radicado + "\n" +
                    "Remitente: " + c.getString(0) + "\n" +
                    "Teléfono Remitente: " + c.getString(1) + "\n" +
                    "Dirección Remitente: " + c.getString(2) + "\n" +
                    "Destinatario: " + c.getString(3) + "\n" +
                    "Teléfono Destinatario: " + c.getString(4) + "\n" +
                    "Dirección Destinatario: " + c.getString(5) + "\n" +
                    "Tamaño: " + c.getString(6) + "\n" +
                    "Peso: " + c.getString(7) + " kg\n" +
                    "Longitud: " + c.getString(8) + " cm\n" +
                    "Método Pago: " + c.getString(9) + "\n" +
                    "Estado: " + c.getString(10) + "\n" +
                    "Tarifa: " + c.getString(11);

            new AlertDialog.Builder(this)
                    .setTitle("Detalles")
                    .setMessage(detalles)
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            Toast.makeText(this, "No se encontraron detalles del paquete", Toast.LENGTH_SHORT).show();
        }
        c.close();
        db.close();
    }

    private void mostrarCalificacion(String radicado) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Calificar Encomienda");

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(1f);
        ratingBar.setRating(0);
        ratingBar.setIsIndicator(false);

        // Limitar el ancho para que solo se vean 5 estrellas
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.width = (int) (getResources().getDisplayMetrics().density * 220); // ~220dp
        ratingBar.setLayoutParams(params);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 0);
        layout.addView(ratingBar);

        builder.setView(layout);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            int calificacion = (int) ratingBar.getRating();
            if (calificacion > 0) {
                guardarCalificacion(radicado, calificacion);
            } else {
                Toast.makeText(this, "Selecciona una calificación", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void guardarCalificacion(String radicado, int calificacion) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("radicado", radicado);
            cv.put("calificacion", calificacion);
            cv.put("comentario", "Calificación desde historial");
            cv.put("fecha", LocalDate.now().toString());

            long resultado = db.insert(DBHelper.TABLE_OPINIONES, null, cv);
            db.close();

            if (resultado != -1) {
                Toast.makeText(this, "Calificado: " + calificacion + " ⭐", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void agregarParadasEnRuta() {
        if (radicadosSeleccionados.isEmpty()) {
            Toast.makeText(this, "No has seleccionado ninguna encomienda", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<String> direcciones = new ArrayList<>();

        for (String radicado : radicadosSeleccionados) {
            Cursor c = db.rawQuery("SELECT destinatarioDireccion FROM " + DBHelper.TABLE_PAQUETES +
                    " WHERE radicado = ?", new String[]{radicado});
            if (c.moveToFirst()) {
                String direccion = c.getString(0);
                if (direccion != null && !direccion.trim().isEmpty()) {
                    direcciones.add(direccion.trim());
                }
            }
            c.close();
        }
        db.close();

        if (direcciones.isEmpty()) {
            Toast.makeText(this, "No se encontraron direcciones válidas para las encomiendas seleccionadas", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String origin = Uri.encode(direcciones.get(0), "UTF-8");
            String destination = Uri.encode(direcciones.get(direcciones.size() - 1), "UTF-8");
            StringBuilder waypoints = new StringBuilder();
            if (direcciones.size() > 2) {
                for (int i = 1; i < direcciones.size() - 1; i++) {
                    if (waypoints.length() > 0) waypoints.append("|");
                    waypoints.append(Uri.encode(direcciones.get(i), "UTF-8"));
                }
            }

            String uri = "https://www.google.com/maps/dir/?api=1" +
                    "&origin=" + origin +
                    "&destination=" + destination +
                    (waypoints.length() > 0 ? "&waypoints=" + waypoints.toString() : "") +
                    "&travelmode=driving";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(intent);
            } catch (Exception e) {
                // Si falla, abrir en navegador
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(browserIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al planear ruta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
