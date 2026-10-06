package com.example.evaluacionmapamain; // Ajusta si en tu proyecto se llama com.example.evalucionmapamain

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;

    // Estado del tipo de marcador seleccionado en el Spinner
    private String tipoSeleccionado = "Policía";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Identificación ante OpenStreetMap
        Configuration.getInstance().setUserAgentValue("EvaluacionApp/1.0 (" + getPackageName() + ")");
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_main);

        // 2. Inicializar MapView con el servidor libre OpenTopo
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.OpenTopo);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 3. Menú desplegable superior
        configurarMenuTipos();

        // 4. Marcadores estáticos predefinidos con tus iconos mipmap
        agregarPuntosFijos();

        // 5. Permitir pulsar en el mapa para añadir nuevos puntos
        habilitarSeleccionDePuntos();

        // 6. Botón de centrado mediante GPS
        FloatingActionButton btnGps = findViewById(R.id.btnGps);
        btnGps.setOnClickListener(v -> verificarPermisosYUbicar());

        verificarPermisosYUbicar();
    }

    private void configurarMenuTipos() {
        Spinner spinner = findViewById(R.id.spinnerTipoPunto);
        String[] opciones = {"Policía", "Accidente"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, opciones);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tipoSeleccionado = opciones[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void agregarPuntosFijos() {
        // Coordenadas fijas de ejemplo (puedes cambiarlas por las que tú elijas)
        GeoPoint coordFija1 = new GeoPoint(-33.4372, -70.6345); // Coordenada Fija 1
        GeoPoint coordFija2 = new GeoPoint(-33.4429, -70.6539); // Coordenada Fija 2

        // Centrar mapa inicialmente en el primer punto
        mapView.getController().setCenter(coordFija1);

        // Marcador Fijo de Policía (Usa el recurso de Image Asset creado en mipmap)
        Marker puntoPolicia = new Marker(mapView);
        puntoPolicia.setPosition(coordFija1);
        puntoPolicia.setTitle("Puesto Fijo: Patrulla Policial");
        puntoPolicia.setSnippet("Vigilancia constante");
        puntoPolicia.setIcon(ContextCompat.getDrawable(this, R.mipmap.ic_policia));
        mapView.getOverlays().add(puntoPolicia);

        // Marcador Fijo de Accidente
        Marker puntoAccidente = new Marker(mapView);
        puntoAccidente.setPosition(coordFija2);
        puntoAccidente.setTitle("Punto Crítico: Accidente Recurrente");
        puntoAccidente.setSnippet("Precaución: zona de alto riesgo");
        puntoAccidente.setIcon(ContextCompat.getDrawable(this, R.mipmap.ic_accidente));
        mapView.getOverlays().add(puntoAccidente);
    }

    private void habilitarSeleccionDePuntos() {
        MapEventsOverlay overlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                crearMarcadorDinamico(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(0, overlay);
    }

    private void crearMarcadorDinamico(GeoPoint punto) {
        Marker nuevoMarcador = new Marker(mapView);
        nuevoMarcador.setPosition(punto);

        Drawable icono;
        if (tipoSeleccionado.equals("Policía")) {
            nuevoMarcador.setTitle("Alerta: Control Policial");
            nuevoMarcador.setSnippet("Lat: " + punto.getLatitude() + ", Lon: " + punto.getLongitude());
            icono = ContextCompat.getDrawable(this, R.mipmap.ic_policia);
        } else {
            nuevoMarcador.setTitle("Alerta: Accidente de Tránsito");
            nuevoMarcador.setSnippet("Lat: " + punto.getLatitude() + ", Lon: " + punto.getLongitude());
            icono = ContextCompat.getDrawable(this, R.mipmap.ic_accidente);
        }

        nuevoMarcador.setIcon(icono);
        mapView.getOverlays().add(nuevoMarcador);
        mapView.invalidate(); // Redibujar mapa para renderizar el nuevo ícono

        Toast.makeText(this, tipoSeleccionado + " agregado al mapa", Toast.LENGTH_SHORT).show();
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

                    // Marcador con la ubicación del usuario
                    Marker miPosicion = new Marker(mapView);
                    miPosicion.setPosition(miUbicacion);
                    miPosicion.setTitle("Mi Ubicación Actual");
                    miPosicion.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation));
                    mapView.getOverlays().add(miPosicion);
                    mapView.invalidate();
                } else {
                    Toast.makeText(this, "Activa el GPS en el dispositivo o emulador", Toast.LENGTH_SHORT).show();
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