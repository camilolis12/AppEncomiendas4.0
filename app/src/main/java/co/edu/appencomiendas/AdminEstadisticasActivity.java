package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.app.AlertDialog; // << agregar import

public class AdminEstadisticasActivity extends AppCompatActivity {

    private DBHelper dbHelper;
    private TextView txtTotalUsuarios, txtTotalPaquetesGlobal, txtPaquetesEntregadosGlobal,
            txtIngresoTotal, txtCalificacionPromedioGlobal, txtPaquetesExpress;
    private ListView listViewMasValor, listViewMejorCalificados;
    private Button btnVerUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_estadisticas);

        dbHelper = new DBHelper(this);

        txtTotalUsuarios = findViewById(R.id.txtTotalUsuarios);
        txtTotalPaquetesGlobal = findViewById(R.id.txtTotalPaquetesGlobal);
        txtPaquetesEntregadosGlobal = findViewById(R.id.txtPaquetesEntregadosGlobal);
        txtIngresoTotal = findViewById(R.id.txtIngresoTotal);
        txtCalificacionPromedioGlobal = findViewById(R.id.txtCalificacionPromedioGlobal);
        txtPaquetesExpress = findViewById(R.id.txtPaquetesExpress);
        listViewMasValor = findViewById(R.id.listViewMasValor);
        listViewMejorCalificados = findViewById(R.id.listViewMejorCalificados);

        // Añadir botón al layout identificado
        LinearLayout layout = findViewById(R.id.layoutAdminEstadisticas);
        btnVerUsuarios = new Button(this);
        btnVerUsuarios.setText("Ver todos los usuarios registrados");
        layout.addView(btnVerUsuarios);

        btnVerUsuarios.setOnClickListener(v -> mostrarUsuariosRegistrados());

        cargarEstadisticasGlobales();
    }

    private void cargarEstadisticasGlobales() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Total usuarios
            Cursor cUsuarios = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_USUARIOS, null);
            cUsuarios.moveToFirst();
            int totalUsuarios = cUsuarios.getInt(0);
            txtTotalUsuarios.setText("Total de usuarios: " + totalUsuarios);
            cUsuarios.close();

            // Total paquetes
            Cursor cPaquetes = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES, null);
            cPaquetes.moveToFirst();
            int totalPaquetes = cPaquetes.getInt(0);
            txtTotalPaquetesGlobal.setText("Total de paquetes: " + totalPaquetes);
            cPaquetes.close();

            // Paquetes entregados
            Cursor cEntregados = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES +
                    " WHERE estado = 'Entregado'", null);
            cEntregados.moveToFirst();
            int paquetesEntregados = cEntregados.getInt(0);
            txtPaquetesEntregadosGlobal.setText("Paquetes entregados: " + paquetesEntregados);
            cEntregados.close();

            // Ingreso total
            Cursor cIngreso = db.rawQuery("SELECT COALESCE(SUM(CAST(REPLACE(REPLACE(tarifa, '$', ''), ',', '') AS INTEGER)), 0) FROM " +
                    DBHelper.TABLE_PAQUETES, null);
            cIngreso.moveToFirst();
            long ingresoTotal = cIngreso.getLong(0);
            txtIngresoTotal.setText("Ingreso total: $" + String.format("%,d", ingresoTotal) + " COP");
            cIngreso.close();

            // Calificación promedio global
            Cursor cCalif = db.rawQuery("SELECT COALESCE(AVG(calificacion), 0) FROM " + DBHelper.TABLE_OPINIONES, null);
            cCalif.moveToFirst();
            double califPromedio = cCalif.getDouble(0);
            txtCalificacionPromedioGlobal.setText(String.format("Calificación promedio: %.1f ⭐", califPromedio));
            cCalif.close();

            // Paquetes Express
            Cursor cExpress = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES +
                    " WHERE express = 1", null);
            cExpress.moveToFirst();
            int paquetesExpress = cExpress.getInt(0);
            txtPaquetesExpress.setText("Paquetes Express: " + paquetesExpress);
            cExpress.close();

            // Top usuarios con más compras
            Cursor cMasValor = db.rawQuery("SELECT remitenteNombre, COUNT(*) as total FROM " +
                    DBHelper.TABLE_PAQUETES + " GROUP BY remitenteNombre ORDER BY total DESC LIMIT 5", null);
            ArrayAdapter<String> adapterMasValor = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            while (cMasValor.moveToNext()) {
                adapterMasValor.add(cMasValor.getString(0) + " (" + cMasValor.getInt(1) + " paquetes)");
            }
            listViewMasValor.setAdapter(adapterMasValor);
            cMasValor.close();

            // Mejores calificados
            Cursor cMejores = db.rawQuery("SELECT radicado, calificacion FROM " + DBHelper.TABLE_OPINIONES +
                    " ORDER BY calificacion DESC LIMIT 5", null);
            ArrayAdapter<String> adapterMejores = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            while (cMejores.moveToNext()) {
                adapterMejores.add("Radicado: " + cMejores.getString(0) + " - " + cMejores.getInt(1) + "⭐");
            }
            listViewMejorCalificados.setAdapter(adapterMejores);
            cMejores.close();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar estadísticas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            db.close();
        }
    }

    private void mostrarUsuariosRegistrados() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT nombreCompleto, usuario, email, rol FROM " + DBHelper.TABLE_USUARIOS + " ORDER BY id", null);
        StringBuilder sb = new StringBuilder();
        while (c.moveToNext()) {
            sb.append("Nombre: ").append(c.getString(0))
              .append("\nUsuario: ").append(c.getString(1))
              .append("\nEmail: ").append(c.getString(2))
              .append("\nRol: ").append(c.getString(3))
              .append("\n\n");
        }
        c.close();
        db.close();
        new AlertDialog.Builder(this)
                .setTitle("Usuarios Registrados")
                .setMessage(sb.length() > 0 ? sb.toString() : "No hay usuarios registrados.")
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
