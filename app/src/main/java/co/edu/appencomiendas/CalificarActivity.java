package co.edu.appencomiendas;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CalificarActivity extends AppCompatActivity {

    RatingBar ratingBar;
    Button btnGuardar;
    int idPaquete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calificar);

        ratingBar = findViewById(R.id.ratingBar);
        btnGuardar = findViewById(R.id.btnGuardar);

        idPaquete = getIntent().getIntExtra("idPaquete", -1);

        btnGuardar.setOnClickListener(v -> {
            int calificacion = (int) ratingBar.getRating();

            if (calificacion == 0) {
                Toast.makeText(this, "Por favor selecciona al menos 1 estrella", Toast.LENGTH_SHORT).show();
                return;
            }

            DBHelper dbHelper = new DBHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put("calificacion", calificacion);

            db.update(DBHelper.TABLE_PAQUETES, values, "id=?", new String[]{String.valueOf(idPaquete)});
            db.close();

            Toast.makeText(this, "Calificación guardada: " + calificacion + " ⭐", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
