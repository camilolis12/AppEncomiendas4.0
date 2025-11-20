package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import android.util.Patterns;

public class RegistroActivity extends AppCompatActivity {

    TextInputEditText edtNombreCompleto, edtUsuario, edtEmail, edtDireccion, edtPassword, edtConfirmarPassword;
    DatePicker datePickerNacimiento;
    RadioGroup rgGenero;
    Spinner spinnerRol;
    Button btnRegistrar;

    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        edtNombreCompleto = findViewById(R.id.edtNombreCompleto);
        edtUsuario = findViewById(R.id.edtUsuario);
        edtEmail = findViewById(R.id.edtEmail);
        edtDireccion = findViewById(R.id.edtDireccion);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmarPassword = findViewById(R.id.edtConfirmarPassword);
        datePickerNacimiento = findViewById(R.id.datePickerNacimiento);
        rgGenero = findViewById(R.id.rgGenero);
        spinnerRol = findViewById(R.id.spinnerRol);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        dbHelper = new DBHelper(this);

        // Llenar spinner de roles
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.roles_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adapter);

        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });
    }

    private void registrarUsuario() {
        String nombre = edtNombreCompleto.getText().toString().trim();
        String usuario = edtUsuario.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String direccion = edtDireccion.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmarPassword = edtConfirmarPassword.getText().toString().trim();

        // Validaciones básicas
        if (nombre.isEmpty() || usuario.isEmpty() || email.isEmpty() || direccion.isEmpty() ||
                password.isEmpty() || confirmarPassword.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmarPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar edad >= 18
        int year = datePickerNacimiento.getYear();
        int month = datePickerNacimiento.getMonth();
        int day = datePickerNacimiento.getDayOfMonth();

        Calendar today = Calendar.getInstance();
        Calendar birthDate = Calendar.getInstance();
        birthDate.set(year, month, day);

        int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);
        if (today.get(Calendar.MONTH) < birthDate.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == birthDate.get(Calendar.MONTH) &&
                        today.get(Calendar.DAY_OF_MONTH) < birthDate.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        if (age < 18) {
            Toast.makeText(this, "Debe ser mayor de 18 años para registrarse", Toast.LENGTH_SHORT).show();
            return;
        }

        // Género
        int selectedGeneroId = rgGenero.getCheckedRadioButtonId();
        if (selectedGeneroId == -1) {
            Toast.makeText(this, "Seleccione un género", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton rbGenero = findViewById(selectedGeneroId);
        String genero = rbGenero.getText().toString();

        // Rol
        String rol = spinnerRol.getSelectedItem().toString();

        // Guardar en SQLite
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("nombreCompleto", nombre);
        values.put("usuario", usuario);
        values.put("email", email);
        values.put("direccion", direccion);
        values.put("password", password);
        values.put("fechaNacimiento", year + "-" + (month + 1) + "-" + day);
        values.put("genero", genero);
        values.put("rol", rol);

        long resultado = db.insert(DBHelper.TABLE_USUARIOS, null, values);

        if (resultado == -1) {
            Toast.makeText(this, "Error: el usuario ya existe", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
            finish(); // cerrar y volver al login
        }

        db.close();
    }
}
