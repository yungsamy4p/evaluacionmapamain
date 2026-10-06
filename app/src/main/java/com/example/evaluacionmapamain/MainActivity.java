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
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;

    private String tipoSeleccionado = "Policía";
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

        // Controles de zoom
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
        mapView.getController().setZoom(15.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        configurarMenuTipos();

        // 1. PUNTOS FIJOS EN LAS COORDENADAS QUE DEFINAS EN EL CÓDIGO
        agregarPuntosFijos();

        // 2. CREACIÓN INTERACTIVA DE PUNTOS
        habilitarSeleccionDePuntos();

        // Botón GPS flotante
        FloatingActionButton btnGps = findViewById(R.id.btnGps);
        btnGps.setOnClickListener(v -> verificarPermisosYUbicar());

        // Botón Limpiar marcadores dinámicos
        Button btnLimpiar = findViewById(R.id.btnLimpiar);
        btnLimpiar.setOnClickListener(v -> limpiarMarcadoresDinamicos());

        // 3. OBTENER UBICACIÓN AUTOMÁTICAMENTE AL INICIAR
        verificarPermisosYUbicar();
    }

    /**
     * AQUÍ PUEDES CAMBIAR LAS COORDENADAS FIJAS QUE TÚ QUIERAS
     */
    private void agregarPuntosFijos() {
        // ===============================================================
        // MODIFICA AQUÍ TUS COORDENADAS (Latitud, Longitud)
        // Ejemplo actual: Coordenadas de Santiago de Chile
        // ===============================================================
        double latPunto1 = -33.4372;
        double lonPunto1 = -70.6345;

        double latPunto2 = -33.4429;
        double lonPunto2 = -70.6539;

        double latPunto3 = -33.4866; // Puedes agregar los puntos fijos que gustes
        double lonPunto3 = -70.6033;
        // ===============================================================

        // Punto Fijo 1: Control Policial
        GeoPoint coord1 = new GeoPoint(latPunto1, lonPunto1);
        Marker punto1 = new Marker(mapView);
        punto1.setPosition(coord1);
        punto1.setTitle("Puesto Fijo: Control Policial");
        punto1.setSnippet(String.format(Locale.getDefault(), "Lat: %.5f | Lon: %.5f", latPunto1, lonPunto1));
        punto1.setIcon(obtenerIconoEscalado(R.mipmap.ic_policia, 55, 55));
        mapView.getOverlays().add(punto1);

        // Punto Fijo 2: Zona de Accidente
        GeoPoint coord2 = new GeoPoint(latPunto2, lonPunto2);
        Marker punto2 = new Marker(mapView);
        punto2.setPosition(coord2);
        punto2.setTitle("Punto Fijo: Accidente Recurrente");
        punto2.setSnippet(String.format(Locale.getDefault(), "Lat: %.5f | Lon: %.5f", latPunto2, lonPunto2));
        punto2.setIcon(obtenerIconoEscalado(R.mipmap.ic_accidente, 55, 55));
        mapView.getOverlays().add(punto2);

        // Centrar inicialmente el mapa en el primer punto fijo antes de que cargue el GPS
        mapView.getController().setCenter(coord1);
    }

    /**
     * OBTIENE LA UBICACIÓN Y MUESTRA COORDENADAS EN PANTALLA Y EN EL MARCADOR
     */
    private void obtenerUbicacionActual() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double miLat = location.getLatitude();
                    double miLon = location.getLongitude();

                    GeoPoint miUbicacion = new GeoPoint(miLat, miLon);
                    mapView.getController().animateTo(miUbicacion);
                    mapView.getController().setZoom(17.0);

                    // Marcador de tu posición actual
                    Marker miPosicion = new Marker(mapView);
                    miPosicion.setPosition(miUbicacion);
                    miPosicion.setTitle("Mi Ubicación Actual");

                    // Texto con las coordenadas exactas dentro del marcador
                    String textoCoordenadas = String.format(Locale.getDefault(), "Lat: %.6f\nLon: %.6f", miLat, miLon);
                    miPosicion.setSnippet(textoCoordenadas);
                    miPosicion.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation));

                    mapView.getOverlays().add(miPosicion);
                    mapView.invalidate();

                    // 1. Abre automáticamente el globito de texto sobre tu ubicación
                    miPosicion.showInfoWindow();

                    // 2. Muestra un mensaje Toast en la pantalla con las coordenadas
                    Toast.makeText(this, "Tu ubicación:\n" + textoCoordenadas, Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(this, "Buscando señal GPS... Asegúrate de tenerlo activado", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
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

        String coords = String.format(Locale.getDefault(), "Lat: %.5f | Lon: %.5f", punto.getLatitude(), punto.getLongitude());

        Drawable icono;
        if (tipoSeleccionado.equals("Policía")) {
            nuevoMarcador.setTitle("Alerta: Policía");
            nuevoMarcador.setSnippet(coords + "\n(Toca para eliminar)");
            icono = obtenerIconoEscalado(R.mipmap.ic_policia, 55, 55);
        } else {
            nuevoMarcador.setTitle("Alerta: Accidente");
            nuevoMarcador.setSnippet(coords + "\n(Toca para eliminar)");
            icono = obtenerIconoEscalado(R.mipmap.ic_accidente, 55, 55);
        }

        nuevoMarcador.setIcon(icono);

        // Eliminar al tocar la ventana informativa
        nuevoMarcador.setOnMarkerClickListener((marker, mapView1) -> {
            marker.showInfoWindow();
            return true;
        });

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

        // Mostrar de inmediato la ventana con sus coordenadas
        nuevoMarcador.showInfoWindow();
    }

    private void limpiarMarcadoresDinamicos() {
        if (marcadoresDinamicos.isEmpty()) {
            Toast.makeText(this, "No hay alertas para borrar", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Limpiar Mapa")
                .setMessage("¿Deseas borrar todas las alertas que colocaste?")
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

    private Drawable obtenerIconoEscalado(int resId, int ancho, int alto) {
        Drawable drawable = ContextCompat.getDrawable(this, resId);
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, ancho, alto, true);
            return new BitmapDrawable(getResources(), scaledBitmap);
        }
        return drawable;
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