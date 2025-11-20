package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class PerfilMenuActivity extends AppCompatActivity {

    Button btnVerPerfil, btnEditarPerfil;
    String usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_menu);

        usuarioActual = getIntent().getStringExtra("usuario");

        btnVerPerfil = findViewById(R.id.btnVerPerfil);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);

        btnVerPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(this, VerPerfilActivity.class);
            intent.putExtra("usuario", usuarioActual);
            startActivity(intent);
        });

        btnEditarPerfil.setOnClickListener(v -> {
            startActivity(new Intent(this, EditarActivity.class));
        });
    }
}
