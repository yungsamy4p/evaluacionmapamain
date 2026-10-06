package com.example.evaluacionmapamain; // Revisa si tu paquete lleva o no la 'a'

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;

    private String tipoSeleccionado = "Policía";

    // Lista para guardar las alertas dinámicas y poder borrarlas
    private final List<Marker> marcadoresDinamicos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue("EvaluacionApp/1.0 (" + getPackageName() + ")");
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.OpenTopo);

        // --- CONTROLES DE ZOOM ---
        mapView.setMultiTouchControls(true); // Zoom táctil con dedos
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS); // Botones visuales + y -
        mapView.getController().setZoom(15.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        configurarMenuTipos();
        agregarPuntosFijos();
        habilitarSeleccionDePuntos();

        // Botón GPS
        FloatingActionButton btnGps = findViewById(R.id.btnGps);
        btnGps.setOnClickListener(v -> verificarPermisosYUbicar());

        // Botón Limpiar Alertas
        Button btnLimpiar = findViewById(R.id.btnLimpiar);
        btnLimpiar.setOnClickListener(v -> limpiarMarcadoresDinamicos());

        verificarPermisosYUbicar();
    }

    /**
     * Reduce el tamaño de cualquier drawable a píxeles definidos
     */
    private Drawable obtenerIconoEscalado(int resId, int ancho, int alto) {
        Drawable drawable = ContextCompat.getDrawable(this, resId);
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, ancho, alto, true);
            return new BitmapDrawable(getResources(), scaledBitmap);
        }
        return drawable;
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
        GeoPoint coordFija1 = new GeoPoint(-33.4372, -70.6345);
        GeoPoint coordFija2 = new GeoPoint(-33.4429, -70.6539);

        mapView.getController().setCenter(coordFija1);

        // Punto fijo 1: Policía (Escalado a 60x60 px)
        Marker puntoPolicia = new Marker(mapView);
        puntoPolicia.setPosition(coordFija1);
        puntoPolicia.setTitle("Puesto Fijo: Patrulla Policial");
        puntoPolicia.setSnippet("Vigilancia constante");
        puntoPolicia.setIcon(obtenerIconoEscalado(R.mipmap.ic_policia, 60, 60));
        mapView.getOverlays().add(puntoPolicia);

        // Punto fijo 2: Accidente (Escalado a 60x60 px)
        Marker puntoAccidente = new Marker(mapView);
        puntoAccidente.setPosition(coordFija2);
        puntoAccidente.setTitle("Punto Crítico: Accidente");
        puntoAccidente.setSnippet("Precaución al transitar");
        puntoAccidente.setIcon(obtenerIconoEscalado(R.mipmap.ic_accidente, 60, 60));
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
            nuevoMarcador.setTitle("Alerta: Policía");
            nuevoMarcador.setSnippet("Toca aquí para eliminar");
            icono = obtenerIconoEscalado(R.mipmap.ic_policia, 55, 55); // Tamaño reducido
        } else {
            nuevoMarcador.setTitle("Alerta: Accidente");
            nuevoMarcador.setSnippet("Toca aquí para eliminar");
            icono = obtenerIconoEscalado(R.mipmap.ic_accidente, 55, 55); // Tamaño reducido
        }

        nuevoMarcador.setIcon(icono);

        // --- OPCIÓN DE ELIMINAR EL MARCADOR INDIVIDUALMENTE ---
        nuevoMarcador.setOnMarkerClickListener((marker, mapView1) -> {
            marker.showInfoWindow();
            return true;
        });

        // Al hacer clic en el globito de texto (InfoWindow), te da la opción de eliminarlo
        nuevoMarcador.getInfoWindow().getView().setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Eliminar Alerta")
                    .setMessage("¿Deseas quitar este punto del mapa?")
                    .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                        mapView.getOverlays().remove(nuevoMarcador);
                        marcadoresDinamicos.remove(nuevoMarcador);
                        nuevoMarcador.closeInfoWindow();
                        mapView.invalidate();
                        Toast.makeText(MainActivity.this, "Punto eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        mapView.getOverlays().add(nuevoMarcador);
        marcadoresDinamicos.add(nuevoMarcador);
        mapView.invalidate();

        Toast.makeText(this, tipoSeleccionado + " añadido", Toast.LENGTH_SHORT).show();
    }

    private void limpiarMarcadoresDinamicos() {
        if (marcadoresDinamicos.isEmpty()) {
            Toast.makeText(this, "No hay alertas para borrar", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Limpiar Mapa")
                .setMessage("¿Deseas borrar todas las alertas que has colocado?")
                .setPositiveButton("Borrar Todo", (dialog, which) -> {
                    for (Marker m : marcadoresDinamicos) {
                        mapView.getOverlays().remove(m);
                        m.closeInfoWindow();
                    }
                    marcadoresDinamicos.clear();
                    mapView.invalidate();
                    Toast.makeText(MainActivity.this, "Mapa limpiado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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