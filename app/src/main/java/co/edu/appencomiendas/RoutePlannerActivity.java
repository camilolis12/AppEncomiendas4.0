package co.edu.appencomiendas;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.net.URLEncoder;
import java.util.ArrayList;

public class RoutePlannerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ArrayList<String> points = new ArrayList<>();

        try {
            // 1) Intentamos obtener direcciones 'Pendiente' con contenido no vacío
            String sql = "SELECT destinatarioDireccion FROM " + DBHelper.TABLE_PAQUETES +
                    " WHERE estado = 'Pendiente' AND destinatarioDireccion IS NOT NULL AND TRIM(destinatarioDireccion) <> ''";
            Cursor c = db.rawQuery(sql, null);
            while (c.moveToNext()) {
                String direccion = c.getString(0);
                if (direccion != null && !direccion.trim().isEmpty()) points.add(direccion.trim());
            }
            c.close();

            // 2) Si no encontramos direcciones con estado, hacemos fallback sin filtrar por estado
            if (points.isEmpty()) {
                Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES, null);
                int total = 0;
                if (totalCursor.moveToFirst()) total = totalCursor.getInt(0);
                totalCursor.close();

                Cursor nonEmpty = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES +
                        " WHERE destinatarioDireccion IS NOT NULL AND TRIM(destinatarioDireccion) <> ''", null);
                int nonEmptyCount = 0;
                if (nonEmpty.moveToFirst()) nonEmptyCount = nonEmpty.getInt(0);
                nonEmpty.close();

                // Si hay direcciones no vacías pero no pertenecen a 'Pendiente', hacemos fallback
                if (nonEmptyCount > 0) {
                    Cursor fallback = db.rawQuery("SELECT destinatarioDireccion, estado FROM " + DBHelper.TABLE_PAQUETES +
                            " WHERE destinatarioDireccion IS NOT NULL AND TRIM(destinatarioDireccion) <> ''", null);
                    ArrayList<String> muestra = new ArrayList<>();
                    while (fallback.moveToNext() && muestra.size() < 5) {
                        String dir = fallback.getString(0);
                        String est = "";
                        try { est = fallback.getString(1); } catch (Exception ignored) {}
                        muestra.add((dir != null ? dir.trim() : "") + " [" + est + "]");
                    }
                    fallback.close();

                    String msg = "No se encontraron direcciones con estado 'Pendiente'.\n" +
                            "Total paquetes: " + total + "\n" +
                            "Paquetes con dirección no vacía: " + nonEmptyCount + "\n" +
                            "Ejemplos (hasta 5):\n" + String.join("\n", muestra) +
                            "\nSe usará fallback: incluir todas las direcciones no vacías.";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

                    // llenar points con todas las direcciones no vacías
                    Cursor all = db.rawQuery("SELECT destinatarioDireccion FROM " + DBHelper.TABLE_PAQUETES +
                            " WHERE destinatarioDireccion IS NOT NULL AND TRIM(destinatarioDireccion) <> ''", null);
                    while (all.moveToNext()) {
                        String direccion = all.getString(0);
                        if (direccion != null && !direccion.trim().isEmpty()) points.add(direccion.trim());
                    }
                    all.close();
                } else {
                    // No hay direcciones en absoluto
                    Toast.makeText(this, "No hay destinos asignados para planear ruta.\nVerifica que los paquetes tengan dirección y estado correcto.", Toast.LENGTH_LONG).show();
                    db.close();
                    finish();
                    return;
                }
            }

            // Mensaje rápido de depuración: cuántos puntos
            Toast.makeText(this, "Paquetes encontrados para ruta: " + points.size(), Toast.LENGTH_SHORT).show();

            // Construir URL de directions
            String origin = URLEncoder.encode(points.get(0), "UTF-8");
            String destination = URLEncoder.encode(points.get(points.size() - 1), "UTF-8");
            StringBuilder waypoints = new StringBuilder();
            if (points.size() > 2) {
                for (int i = 1; i < points.size() - 1; i++) {
                    if (waypoints.length() > 0) waypoints.append("|");
                    waypoints.append(URLEncoder.encode(points.get(i), "UTF-8"));
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
            Toast.makeText(this, "Error al planear ruta: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.close();
            finish();
        }
    }
}
