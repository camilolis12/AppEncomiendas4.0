package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class ModificarActivity extends AppCompatActivity {

    EditText edtRadicadoBuscar, edtDestinatarioNombre, edtDestinatarioDireccion, edtDestinatarioTelefono;
    Button btnBuscarModificar, btnGuardarCambios;

    String radicadoActual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modificar);

        edtRadicadoBuscar = findViewById(R.id.edtRadicadoBuscar);
        edtDestinatarioNombre = findViewById(R.id.edtDestinatarioNombre);
        edtDestinatarioDireccion = findViewById(R.id.edtDestinatarioDireccion);
        edtDestinatarioTelefono = findViewById(R.id.edtDestinatarioTelefono);

        btnBuscarModificar = findViewById(R.id.btnBuscarModificar);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);

        btnBuscarModificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscarPaquete();
            }
        });

        btnGuardarCambios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarCambios();
            }
        });
    }

    private void buscarPaquete() {
        radicadoActual = edtRadicadoBuscar.getText().toString().trim();

        if (radicadoActual.isEmpty()) {
            Toast.makeText(this, "Ingrese un número de radicado", Toast.LENGTH_SHORT).show();
            return;
        }

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT destinatarioNombre, destinatarioDireccion, destinatarioTelefono " +
                "FROM " + DBHelper.TABLE_PAQUETES + " WHERE radicado = ?", new String[]{radicadoActual});

        if (cursor.moveToFirst()) {
            edtDestinatarioNombre.setText(cursor.getString(0));
            edtDestinatarioDireccion.setText(cursor.getString(1));
            edtDestinatarioTelefono.setText(cursor.getString(2));
        } else {
            Toast.makeText(this, "Paquete no encontrado", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        db.close();
    }

    private void guardarCambios() {
        if (radicadoActual.isEmpty()) {
            Toast.makeText(this, "Primero busque un paquete", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = edtDestinatarioNombre.getText().toString().trim();
        String direccion = edtDestinatarioDireccion.getText().toString().trim();
        String telefono = edtDestinatarioTelefono.getText().toString().trim();

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("destinatarioNombre", nombre);
        values.put("destinatarioDireccion", direccion);
        values.put("destinatarioTelefono", telefono);

        int rows = db.update(DBHelper.TABLE_PAQUETES, values, "radicado = ?", new String[]{radicadoActual});

        if (rows > 0) {
            Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
        }

        db.close();
    }
}
