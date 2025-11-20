package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.content.ContentValues; // << agregar import

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.net.Uri;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.Priority;

public class MainActivity extends AppCompatActivity {

    TextInputEditText edtUsuario, edtPassword;
    Button btnLogin, btnRegistro, btnUbicacionActual;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtUsuario = findViewById(R.id.edtUsuario);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegistro = findViewById(R.id.btnRegistro);
        btnUbicacionActual = findViewById(R.id.btnUbicacionActual);

        dbHelper = new DBHelper(this);

        // Crear usuario admin si no existe
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM " + DBHelper.TABLE_USUARIOS + " WHERE usuario = ?", new String[]{"admin"});
        if (!c.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put("nombreCompleto", "Administrador");
            values.put("usuario", "admin");
            values.put("email", "admin@encomiendas.com");
            values.put("direccion", "Oficina Central");
            values.put("password", "1234");
            values.put("fechaNacimiento", "1980-01-01");
            values.put("genero", "Binario");
            values.put("rol", "Administrador");
            values.put("telefono", "0000000000");
            values.put("notif_pref", "email");
            db.insert(DBHelper.TABLE_USUARIOS, null, values);
        }
        c.close();
        db.close();

        // Botón login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarSesion();
            }
        });

        // Botón registro
        btnRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, RegistroActivity.class);
                startActivity(i);
            }
        });

        // Botón ubicación actual
        btnUbicacionActual.setOnClickListener(v -> mostrarUbicacionActual());
    }

    private void iniciarSesion() {
        String usuario = edtUsuario.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (usuario.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM " + DBHelper.TABLE_USUARIOS +
                        " WHERE usuario = ? AND password = ?",
                new String[]{usuario, password});

        if (cursor.moveToFirst()) {
            String usuarioLog = usuario;
            // Si es admin, ir a estadísticas globales
            if ("admin".equals(usuarioLog)) {
                Intent i = new Intent(MainActivity.this, AdminEstadisticasActivity.class);
                i.putExtra("usuario", usuarioLog);
                startActivity(i);
                finish();
                cursor.close();
                db.close();
                return;
            }
            // Usuario normal → ir a SeccionActivity
            Intent i = new Intent(MainActivity.this, SeccionActivity.class);
            i.putExtra("usuario", usuarioLog);
            startActivity(i);
            finish();
        } else {
            Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        db.close();
    }

    private void mostrarUbicacionActual() {
        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedClient.getCurrentLocation(req, null).addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                Intent intent = new Intent(MainActivity.this, MapaActivity.class);
                intent.putExtra("latitud", lat);
                intent.putExtra("longitud", lng);
                intent.putExtra("radicado", "Mi ubicación");
                startActivity(intent);
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
