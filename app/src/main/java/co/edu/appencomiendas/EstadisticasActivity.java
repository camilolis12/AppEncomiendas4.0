package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class EstadisticasActivity extends AppCompatActivity {

    private String usuarioActual;
    private DBHelper dbHelper;
    private TextView txtTotalPaquetes, txtPaquetesEntregados, txtPaquetesPendientes,
            txtGastoTotal, txtCalificacionPromedio, txtOpinionesRegistradas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estadisticas);

        usuarioActual = getIntent().getStringExtra("usuario");
        dbHelper = new DBHelper(this);

        txtTotalPaquetes = findViewById(R.id.txtTotalPaquetes);
        txtPaquetesEntregados = findViewById(R.id.txtPaquetesEntregados);
        txtPaquetesPendientes = findViewById(R.id.txtPaquetesPendientes);
        txtGastoTotal = findViewById(R.id.txtGastoTotal);
        txtCalificacionPromedio = findViewById(R.id.txtCalificacionPromedio);
        txtOpinionesRegistradas = findViewById(R.id.txtOpinionesRegistradas);

        cargarEstadisticas();
    }

    private void cargarEstadisticas() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            // Obtener nombre completo del usuario actual
            String nombreCompleto = "";
            Cursor cNombre = db.rawQuery("SELECT nombreCompleto FROM " + DBHelper.TABLE_USUARIOS + " WHERE usuario = ?", new String[]{usuarioActual});
            if (cNombre.moveToFirst()) {
                nombreCompleto = cNombre.getString(0);
            }
            cNombre.close();

            // Total de paquetes
            Cursor cTotal = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES +
                    " WHERE remitenteNombre = ?", new String[]{nombreCompleto});
            cTotal.moveToFirst();
            int totalPaquetes = cTotal.getInt(0);
            txtTotalPaquetes.setText("Total de paquetes: " + totalPaquetes);
            cTotal.close();

            // Paquetes entregados
            Cursor cEntregados = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES +
                    " WHERE remitenteNombre = ? AND estado = 'Entregado'", new String[]{nombreCompleto});
            cEntregados.moveToFirst();
            int paquetesEntregados = cEntregados.getInt(0);
            txtPaquetesEntregados.setText("Paquetes entregados: " + paquetesEntregados);
            cEntregados.close();

            // Paquetes pendientes
            Cursor cPendientes = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_PAQUETES +
                    " WHERE remitenteNombre = ? AND estado = 'Pendiente'", new String[]{nombreCompleto});
            cPendientes.moveToFirst();
            int paquetesPendientes = cPendientes.getInt(0);
            txtPaquetesPendientes.setText("Paquetes pendientes: " + paquetesPendientes);
            cPendientes.close();

            // Gasto total
            Cursor cGasto = db.rawQuery("SELECT COALESCE(SUM(CAST(REPLACE(REPLACE(tarifa, '$', ''), ',', '') AS INTEGER)), 0) FROM " +
                    DBHelper.TABLE_PAQUETES + " WHERE remitenteNombre = ?", new String[]{nombreCompleto});
            cGasto.moveToFirst();
            long gastoTotal = cGasto.getLong(0);
            txtGastoTotal.setText("Gasto total: $" + String.format("%,d", gastoTotal) + " COP");
            cGasto.close();

            // Calificación promedio
            Cursor cCalif = db.rawQuery("SELECT COALESCE(AVG(calificacion), 0) FROM " + DBHelper.TABLE_OPINIONES +
                    " WHERE usuario = ?", new String[]{usuarioActual});
            cCalif.moveToFirst();
            double califPromedio = cCalif.getDouble(0);
            txtCalificacionPromedio.setText(String.format("Calificación promedio: %.1f ⭐", califPromedio));
            cCalif.close();

            // Opiniones registradas
            Cursor cOpiniones = db.rawQuery("SELECT COUNT(*) FROM " + DBHelper.TABLE_OPINIONES +
                    " WHERE usuario = ?", new String[]{usuarioActual});
            cOpiniones.moveToFirst();
            int opiniones = cOpiniones.getInt(0);
            txtOpinionesRegistradas.setText("Opiniones registradas: " + opiniones);
            cOpiniones.close();
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar estadísticas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            db.close();
        }
    }
}
