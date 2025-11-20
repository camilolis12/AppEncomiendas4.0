package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.content.ContentValues;

public class EditarActivity extends AppCompatActivity {

    EditText edtUsuario, edtContrasenaActual, edtNuevoCorreo, edtNuevaDireccion, edtNuevaContrasena;
    Button btnGuardar;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar);

        edtUsuario = findViewById(R.id.edtUsuario);
        edtContrasenaActual = findViewById(R.id.edtContrasenaActual);
        edtNuevoCorreo = findViewById(R.id.edtNuevoCorreo);
        edtNuevaDireccion = findViewById(R.id.edtNuevaDireccion);
        edtNuevaContrasena = findViewById(R.id.edtNuevaContrasena);
        btnGuardar = findViewById(R.id.btnGuardar);

        dbHelper = new DBHelper(this);

        btnGuardar.setOnClickListener(v -> {
            String usuario = edtUsuario.getText().toString().trim();
            String contrasenaActual = edtContrasenaActual.getText().toString().trim();
            String nuevoCorreo = edtNuevoCorreo.getText().toString().trim();
            String nuevaDireccion = edtNuevaDireccion.getText().toString().trim();
            String nuevaContrasena = edtNuevaContrasena.getText().toString().trim();

            // Validación de campos obligatorios
            if (usuario.isEmpty() || contrasenaActual.isEmpty()) {
                Toast.makeText(this, "Usuario y contraseña actual son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = null;
            Cursor cursor = null;
            Cursor check = null;
            try {
                db = dbHelper.getWritableDatabase();

                // Verificar que la tabla de usuarios exista
                check = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{DBHelper.TABLE_USUARIOS});
                if (check == null || !check.moveToFirst()) {
                    Toast.makeText(this, "La base de datos no está inicializada correctamente. Reinstala la app o borra datos.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Verificar que usuario y contraseña actual existan (selecciono id para economizar)
                cursor = db.rawQuery("SELECT id FROM " + DBHelper.TABLE_USUARIOS + " WHERE usuario = ? AND password = ?",
                        new String[]{usuario, contrasenaActual});

                if (cursor != null && cursor.moveToFirst()) {
                    // Si el usuario existe, proceder a actualizar
                    ContentValues values = new ContentValues();

                    if (!nuevoCorreo.isEmpty()) values.put("email", nuevoCorreo);
                    if (!nuevaDireccion.isEmpty()) values.put("direccion", nuevaDireccion);
                    if (!nuevaContrasena.isEmpty()) values.put("password", nuevaContrasena);

                    if (values.size() == 0) {
                        Toast.makeText(this, "No hay cambios para actualizar", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int filas = db.update(DBHelper.TABLE_USUARIOS, values, "usuario = ?", new String[]{usuario});

                    if (filas > 0) {
                        Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "No se pudo actualizar el usuario", Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(this, "Usuario o contraseña actual incorrectos", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error en la base de datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                if (check != null) check.close();
                if (cursor != null) cursor.close();
                if (db != null) db.close();
            }
        });
    }
}
