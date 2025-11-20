package co.edu.appencomiendas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class SeccionActivity extends AppCompatActivity {

    private Button btnRegistrarEnvio, btnRastrear, btnServicio, btnModificar,
                   btnHistorial, btnPerfil, btnFavoritos, btnEstadisticas, btnAdminEstadisticas, btnPlanearRuta;
    private String usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seccion);

        usuarioActual = getIntent().getStringExtra("usuario");

        initializeButtons();
        setupListeners();

        // Ocultar botón de estadísticas generales si no es admin
        if (!"admin".equals(usuarioActual)) {
            btnAdminEstadisticas.setVisibility(Button.GONE);
        }
    }

    private void initializeButtons() {
        btnRegistrarEnvio = findViewById(R.id.btnRegistrarEnvio);
        btnRastrear = findViewById(R.id.btnRastrear);
        btnServicio = findViewById(R.id.btnServicio);
        btnModificar = findViewById(R.id.btnModificar);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnPerfil = findViewById(R.id.btnPerfil);
        btnFavoritos = findViewById(R.id.btnFavoritos);
        btnEstadisticas = findViewById(R.id.btnEstadisticas);
        btnAdminEstadisticas = findViewById(R.id.btnAdminEstadisticas);
        btnPlanearRuta = findViewById(R.id.btnPlanearRuta);
    }

    private void setupListeners() {
        btnRegistrarEnvio.setOnClickListener(v -> abrirFormulario());
        btnRastrear.setOnClickListener(v -> abrirRastrear());
        btnServicio.setOnClickListener(v -> abrirServicio());
        btnModificar.setOnClickListener(v -> abrirModificar());
        btnHistorial.setOnClickListener(v -> abrirHistorial());
        btnPerfil.setOnClickListener(v -> abrirPerfil());
        btnFavoritos.setOnClickListener(v -> abrirFavoritos());
        btnEstadisticas.setOnClickListener(v -> abrirEstadisticas());
        btnAdminEstadisticas.setOnClickListener(v -> abrirAdminEstadisticas());
        btnPlanearRuta.setOnClickListener(v -> {
            Intent intent = new Intent(this, RoutePlannerActivity.class);
            startActivity(intent);
        });
    }

    private void abrirFormulario() {
        Intent intent = new Intent(this, FormularioActivity.class);
        intent.putExtra("usuario", usuarioActual);
        startActivity(intent);
    }

    private void abrirRastrear() {
        Intent intent = new Intent(this, RastrearActivity.class);
        intent.putExtra("usuario", usuarioActual);
        startActivity(intent);
    }

    private void abrirServicio() {
        Intent intent = new Intent(this, ServicioActivity.class);
        startActivity(intent);
    }

    private void abrirModificar() {
        Intent intent = new Intent(this, ModificarActivity.class);
        intent.putExtra("usuario", usuarioActual);
        startActivity(intent);
    }

    private void abrirHistorial() {
        Intent intent = new Intent(this, HistorialActivity.class);
        intent.putExtra("usuario", usuarioActual);
        startActivity(intent);
    }

    private void abrirPerfil() {
        Intent intent = new Intent(this, VerPerfilActivity.class);
        intent.putExtra("usuario", usuarioActual);
        startActivity(intent);
    }

    private void abrirFavoritos() {
        Intent intent = new Intent(this, FavoritesActivity.class);
        intent.putExtra("usuario", usuarioActual);
        startActivity(intent);
    }

    private void abrirEstadisticas() {
        Intent intent = new Intent(this, EstadisticasActivity.class);
        intent.putExtra("usuario", usuarioActual);
        startActivity(intent);
    }

    private void abrirAdminEstadisticas() {
        Intent intent = new Intent(this, AdminEstadisticasActivity.class);
        startActivity(intent);
    }
}
