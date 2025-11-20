package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.app.AlertDialog;

import java.util.ArrayList;
import com.google.android.material.textfield.TextInputEditText;

public class FavoritesActivity extends AppCompatActivity {

    TextInputEditText edtFavNombre, edtFavDireccion, edtFavTelefono;
    Button btnAddFav, btnClearFav;
    ListView listViewFavoritos;
    ArrayList<FavItem> favoritos;
    ArrayAdapter<String> adapter;
    DBHelper dbHelper;
    private String usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favoritos);

        edtFavNombre = findViewById(R.id.edtFavNombre);
        edtFavDireccion = findViewById(R.id.edtFavDireccion);
        edtFavTelefono = findViewById(R.id.edtFavTelefono);
        btnAddFav = findViewById(R.id.btnAddFav);
        btnClearFav = findViewById(R.id.btnClearFav);
        listViewFavoritos = findViewById(R.id.listViewFavoritos);

        dbHelper = new DBHelper(this);
        favoritos = new ArrayList<>();

        // Asegurar que la tabla 'favoritos' exista en la BD (por si la BD fue creada con esquema anterior)
        SQLiteDatabase ensureDb = dbHelper.getWritableDatabase();
        dbHelper.ensureFavoritosTable(ensureDb);
        ensureDb.close();

        usuarioActual = getIntent().getStringExtra("usuario");
        if (usuarioActual == null) {
            usuarioActual = ""; // fallback para evitar bind null
            Toast.makeText(this, "Aviso: usuario no identificado. Se mostrarán favoritos globales.", Toast.LENGTH_SHORT).show();
        }

        btnAddFav.setOnClickListener(v -> addFavorite());
        btnClearFav.setOnClickListener(v -> {
            edtFavNombre.setText("");
            edtFavDireccion.setText("");
            edtFavTelefono.setText("");
        });

        listViewFavoritos.setOnItemLongClickListener((parent, view, position, id) -> {
            FavItem item = favoritos.get(position);
            confirmDelete(item.id);
            return true;
        });

        loadFavorites();
    }

    private void addFavorite() {
        if (usuarioActual.isEmpty()) {
            Toast.makeText(this, "No está identificado el usuario. Inicia sesión para guardar favoritos.", Toast.LENGTH_SHORT).show();
            return;
        }
        String nombre = edtFavNombre.getText().toString().trim();
        String direccion = edtFavDireccion.getText().toString().trim();
        String telefono = edtFavTelefono.getText().toString().trim();
        if (nombre.isEmpty() || direccion.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Nombre, dirección y teléfono obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("nombre", nombre);
        cv.put("direccion", direccion);
        cv.put("telefono", telefono);
        cv.put("latitud", 0);
        cv.put("longitud", 0);
        cv.put("tipo", "personal");
        cv.put("usuario", usuarioActual);
        long id = db.insert(DBHelper.TABLE_FAVORITOS, null, cv);
        db.close();

        if (id != -1) {
            Toast.makeText(this, "Favorito agregado", Toast.LENGTH_SHORT).show();
            edtFavNombre.setText("");
            edtFavDireccion.setText("");
            edtFavTelefono.setText("");
            loadFavorites();
        } else {
            Toast.makeText(this, "Error al agregar favorito", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFavorites() {
        favoritos.clear();
        ArrayList<String> names = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c;
        if (usuarioActual.isEmpty()) {
            // fallback: mostrar todos
            c = db.rawQuery("SELECT id, nombre, direccion, telefono FROM " + DBHelper.TABLE_FAVORITOS + " ORDER BY id DESC", null);
        } else {
            c = db.rawQuery("SELECT id, nombre, direccion, telefono FROM " + DBHelper.TABLE_FAVORITOS + " WHERE usuario = ? ORDER BY id DESC", new String[]{usuarioActual});
        }

        if (c.moveToFirst()) {
            do {
                int id = c.getInt(0);
                String nombre = c.getString(1);
                String direccion = c.getString(2);
                String telefono = c.getString(3);
                favoritos.add(new FavItem(id, nombre, direccion, telefono));
                names.add(nombre + " — " + direccion + " — " + telefono);
            } while (c.moveToNext());
        }
        c.close();
        db.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listViewFavoritos.setAdapter(adapter);
    }

    private void confirmDelete(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar favorito")
                .setMessage("¿Eliminar esta dirección favorita?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int rows = db.delete(DBHelper.TABLE_FAVORITOS, "id = ?", new String[]{String.valueOf(id)});
                    db.close();
                    if (rows > 0) {
                        Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show();
                        loadFavorites();
                    } else {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    static class FavItem {
        int id; String nombre, direccion, telefono;
        FavItem(int id, String nombre, String direccion, String telefono) {
            this.id = id; this.nombre = nombre; this.direccion = direccion; this.telefono = telefono;
        }
    }
}
