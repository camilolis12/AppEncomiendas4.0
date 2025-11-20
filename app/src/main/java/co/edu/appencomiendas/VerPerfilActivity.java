package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;

public class VerPerfilActivity extends AppCompatActivity {

    private EditText edtVerificarPassword, edtTelefonoPerfil;
    private Button btnVerificar, btnGuardarPerfil;
    private LinearLayout layoutVerificacion, layoutPerfil;
    private TextView txtNombreCompleto, txtUsuario, txtEmail, txtDireccion,
            txtFechaNacimiento, txtGenero, txtRol;
    private Spinner spinnerNotifPref;

    private String usuarioActual;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_perfil);

        // Obtener el usuario actual del intent
        usuarioActual = getIntent().getStringExtra("usuario");
        dbHelper = new DBHelper(this);

        initializeViews();

        // poblar spinner de preferencias
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"Email", "SMS", "Ambas"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotifPref.setAdapter(adapter);

        btnVerificar.setOnClickListener(v -> verificarPassword());
        btnGuardarPerfil.setOnClickListener(v -> guardarPreferencias());
    }

    private void initializeViews() {
        edtVerificarPassword = findViewById(R.id.edtVerificarPassword);
        btnVerificar = findViewById(R.id.btnVerificar);
        layoutVerificacion = findViewById(R.id.layoutVerificacion);
        layoutPerfil = findViewById(R.id.layoutPerfil);

        txtNombreCompleto = findViewById(R.id.txtNombreCompleto);
        txtUsuario = findViewById(R.id.txtUsuario);
        txtEmail = findViewById(R.id.txtEmail);
        edtTelefonoPerfil = findViewById(R.id.edtTelefonoPerfil);
        txtDireccion = findViewById(R.id.txtDireccion);
        txtFechaNacimiento = findViewById(R.id.txtFechaNacimiento);
        txtGenero = findViewById(R.id.txtGenero);
        txtRol = findViewById(R.id.txtRol);
        spinnerNotifPref = findViewById(R.id.spinnerNotifPref);
        btnGuardarPerfil = findViewById(R.id.btnGuardarPerfil);
    }

    private void verificarPassword() {
        String password = edtVerificarPassword.getText().toString().trim();

        if (password.isEmpty()) {
            Toast.makeText(this, "Ingresa tu contraseña", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT nombreCompleto, usuario, email, direccion, fechaNacimiento, genero, rol, telefono, notif_pref FROM "
                + DBHelper.TABLE_USUARIOS + " WHERE usuario = ? AND password = ?", new String[]{usuarioActual, password});

        if (cursor.moveToFirst()) {
            txtNombreCompleto.setText("Nombre: " + cursor.getString(0));
            txtUsuario.setText("Usuario: " + cursor.getString(1));
            txtEmail.setText("Email: " + cursor.getString(2));
            txtDireccion.setText("Dirección: " + cursor.getString(3));
            txtFechaNacimiento.setText("Fecha de nacimiento: " + cursor.getString(4));
            txtGenero.setText("Género: " + cursor.getString(5));
            txtRol.setText("Rol: " + cursor.getString(6));

            String tel = cursor.isNull(7) ? "" : cursor.getString(7);
            String pref = cursor.isNull(8) ? "email" : cursor.getString(8);

            edtTelefonoPerfil.setText(tel);
            // map pref -> spinner index
            if ("sms".equalsIgnoreCase(pref)) spinnerNotifPref.setSelection(1);
            else if ("both".equalsIgnoreCase(pref)) spinnerNotifPref.setSelection(2);
            else spinnerNotifPref.setSelection(0);

            layoutVerificacion.setVisibility(View.GONE);
            layoutPerfil.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
        db.close();
    }

    private void guardarPreferencias() {
        String telefono = edtTelefonoPerfil.getText().toString().trim();
        String sel = spinnerNotifPref.getSelectedItem().toString();
        String pref = "email";
        if ("SMS".equals(sel)) pref = "sms";
        else if ("Ambas".equals(sel)) pref = "both";

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("telefono", telefono);
        cv.put("notif_pref", pref);

        int rows = db.update(DBHelper.TABLE_USUARIOS, cv, "usuario = ?", new String[]{usuarioActual});
        db.close();

        if (rows > 0) {
            Toast.makeText(this, "Preferencias actualizadas", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al guardar preferencias", Toast.LENGTH_SHORT).show();
        }
    }
}
