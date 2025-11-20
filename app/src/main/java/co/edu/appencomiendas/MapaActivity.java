package co.edu.appencomiendas;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class MapaActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQ_LOCATION = 100;

    private GoogleMap googleMap;
    private TextView txtRadicadoMapa;
    private double latitud, longitud;
    private String radicado;
    private List<LatLng> trackingPoints = new ArrayList<>();

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
            cargarTrackingPoints(radicado);
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

        if (!trackingPoints.isEmpty()) {
            googleMap.addPolyline(new PolylineOptions()
                    .addAll(trackingPoints)
                    .color(Color.BLUE)
                    .width(8f));

            LatLng inicio = trackingPoints.get(0);
            LatLng ultimo = trackingPoints.get(trackingPoints.size() - 1);
            googleMap.addMarker(new MarkerOptions().position(inicio).title("Inicio recorrido"));
            googleMap.addMarker(new MarkerOptions().position(ultimo).title("Último reporte"));
            punto = ultimo;
        } else {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(punto)
                    .title(radicado != null ? ("Paquete " + radicado) : "Paquete");
            googleMap.addMarker(markerOptions);
            Toast.makeText(this, "Sin puntos de tracking, se muestra la última ubicación guardada", Toast.LENGTH_SHORT).show();
        }

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

    private void cargarTrackingPoints(String radicado) {
        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT latitud, longitud FROM tracking_points WHERE radicado = ? ORDER BY id ASC", new String[]{radicado});
        trackingPoints.clear();
        while (cursor.moveToNext()) {
            if (!cursor.isNull(0) && !cursor.isNull(1)) {
                trackingPoints.add(new LatLng(cursor.getDouble(0), cursor.getDouble(1)));
            }
        }
        cursor.close();
        db.close();
    }
}
