package co.edu.appencomiendas;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MapaActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQ_LOCATION = 100;

    private GoogleMap googleMap;
    private TextView txtRadicadoMapa;
    private double latitud, longitud;
    private String radicado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);

        txtRadicadoMapa = findViewById(R.id.txtRadicadoMapa);

        // Recibir valores enviados desde RastrearActivity
        latitud = getIntent().getDoubleExtra("latitud", 4.7110);   // Bogotá por defecto
        longitud = getIntent().getDoubleExtra("longitud", -74.0721);
        radicado = getIntent().getStringExtra("radicado");

        // Si no hay coordenadas, mostrar Bogotá
        if (latitud == 0 && longitud == 0) {
            latitud = 4.7110;
            longitud = -74.0721;
        }

        if (radicado != null && !radicado.isEmpty()) {
            txtRadicadoMapa.setText("Paquete " + radicado);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Error al inicializar el mapa", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        LatLng punto = new LatLng(latitud, longitud);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(punto)
                .title(radicado != null ? ("Paquete " + radicado) : "Paquete");
        googleMap.addMarker(markerOptions);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(punto, 15f));

        // Intento activar capa My Location si hay permiso
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException se) {
                // no debería ocurrir si permiso fue concedido
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException ignored) {}
                }
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
