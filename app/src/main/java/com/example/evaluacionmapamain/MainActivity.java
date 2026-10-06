package com.example.evalucionmapamain;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true); // Permite zoom con los dedos
        mapView.getController().setZoom(15.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //  PUNTOS FIJOS
        agregarPuntosFijos();

        //  AÑADIR PUNTOS SELECCIONABLES (Click o pulsación larga en el mapa)
        habilitarSeleccionDePuntos();

        //  GEOLOCALIZACIÓN
        FloatingActionButton btnGps = findViewById(R.id.btnGps);
        btnGps.setOnClickListener(v -> verificarPermisosYUbicar());

        verificarPermisosYUbicar();
    }

    private void agregarPuntosFijos() {
        // Ejemplo de punto fijo
        GeoPoint puntoInicial = new GeoPoint(-33.4569, -70.6483);
        mapView.getController().setCenter(puntoInicial);

        Marker marcadorFijo = new Marker(mapView);
        marcadorFijo.setPosition(puntoInicial);
        marcadorFijo.setTitle("Punto Fijo 1");
        marcadorFijo.setSnippet("Ubicación de referencia asignada");

        // Asignar icono personalizado del sistema
        marcadorFijo.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_dialog_map));
        mapView.getOverlays().add(marcadorFijo);
    }

    private void habilitarSeleccionDePuntos() {
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Al pulsar un lugar del mapa, se agrega un nuevo marcador
                crearMarcadorDinamico(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void crearMarcadorDinamico(GeoPoint punto) {
        Marker nuevoMarcador = new Marker(mapView);
        nuevoMarcador.setPosition(punto);
        nuevoMarcador.setTitle("Punto Seleccionado");
        nuevoMarcador.setSnippet("Lat: " + punto.getLatitude() + ", Lon: " + punto.getLongitude());

        // Icono diferenciado para puntos seleccionados por el usuario
        nuevoMarcador.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_input_add));

        mapView.getOverlays().add(nuevoMarcador);
        mapView.invalidate(); // Refresca el mapa para pintar el nuevo icono
        Toast.makeText(this, "Marcador añadido en la posición tocada", Toast.LENGTH_SHORT).show();
    }

    private void verificarPermisosYUbicar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            obtenerUbicacionActual();
        }
    }

    private void obtenerUbicacionActual() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    GeoPoint miUbicacion = new GeoPoint(location.getLatitude(), location.getLongitude());
                    mapView.getController().animateTo(miUbicacion);
                    mapView.getController().setZoom(17.0);

                    // Marcador de posición actual con icono de geolocalización
                    Marker miPosicionMarker = new Marker(mapView);
                    miPosicionMarker.setPosition(miUbicacion);
                    miPosicionMarker.setTitle("Mi Ubicación Actual");
                    miPosicionMarker.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation));

                    mapView.getOverlays().add(miPosicionMarker);
                    mapView.invalidate();
                } else {
                    Toast.makeText(this, "Asegúrate de activar el GPS en el dispositivo", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacionActual();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}