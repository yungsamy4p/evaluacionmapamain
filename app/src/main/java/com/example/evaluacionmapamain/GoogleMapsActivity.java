package com.example.evaluacionmapamain;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoogleMapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String tipoSeleccionado = "Policía";
    private final List<Marker> marcadoresDinamicos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_maps);

        // Inicializar fragmento de Google Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapGoogle);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Configuración de selector y botón limpiar
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggleGroupGoogle);
        toggleGroup.check(R.id.btnGMapPolicia);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                tipoSeleccionado = (checkedId == R.id.btnGMapPolicia) ? "Policía" : "Accidente";
            }
        });

        MaterialButton btnLimpiar = findViewById(R.id.btnLimpiarGoogle);
        btnLimpiar.setOnClickListener(v -> limpiarMarcadores());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Activar controles de zoom nativos de Google Maps
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 1. Centrar en tu coordenada establecida
        LatLng miUbicacion = new LatLng(-33.514043466757705, -70.58495472492446);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 16.0f));

        Marker miPos = mMap.addMarker(new MarkerOptions()
                .position(miUbicacion)
                .title("Mi Ubicación Actual")
                .snippet(String.format(Locale.getDefault(), "Lat: %.6f | Lon: %.6f", miUbicacion.latitude, miUbicacion.longitude)));
        if (miPos != null) miPos.showInfoWindow();

        // 2. Puntos fijos
        agregarPuntosFijos();

        // 3. Click sobre el mapa para colocar alertas interactivas
        mMap.setOnMapClickListener(this::crearMarcadorDinamico);

        // 4. Click en ventana para eliminar marcador dinámico
        mMap.setOnInfoWindowClickListener(marker -> {
            if (marcadoresDinamicos.contains(marker)) {
                new AlertDialog.Builder(this)
                        .setTitle("Eliminar Alerta")
                        .setMessage("¿Deseas quitar este punto del mapa?")
                        .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                            marcadoresDinamicos.remove(marker);
                            marker.remove();
                            Toast.makeText(this, "Punto eliminado", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
    }

    private void agregarPuntosFijos() {
        LatLng pFijo1 = new LatLng(-33.510151, -70.588325);
        LatLng pFijo2 = new LatLng(-33.510353, -70.582411);

        mMap.addMarker(new MarkerOptions()
                .position(pFijo1)
                .title("Puesto Fijo: Control Policial")
                .snippet("Vigilancia constante")
                .icon(crearIconoBitmap(R.mipmap.ic_policia, 75, 75)));

        mMap.addMarker(new MarkerOptions()
                .position(pFijo2)
                .title("Punto Fijo: Accidente")
                .snippet("Zona crítica")
                .icon(crearIconoBitmap(R.mipmap.ic_accidente, 75, 75)));
    }

    private void crearMarcadorDinamico(LatLng latLng) {
        BitmapDescriptor icon = tipoSeleccionado.equals("Policía")
                ? crearIconoBitmap(R.mipmap.ic_policia, 70, 70)
                : crearIconoBitmap(R.mipmap.ic_accidente, 70, 70);

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Alerta: " + tipoSeleccionado)
                .snippet(String.format(Locale.getDefault(), "Lat: %.5f | Lon: %.5f (Toca para borrar)", latLng.latitude, latLng.longitude))
                .icon(icon));

        if (marker != null) {
            marcadoresDinamicos.add(marker);
            marker.showInfoWindow();
        }
        Toast.makeText(this, tipoSeleccionado + " agregado", Toast.LENGTH_SHORT).show();
    }

    private void limpiarMarcadores() {
        if (marcadoresDinamicos.isEmpty()) {
            Toast.makeText(this, "No hay alertas para borrar", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Marker m : marcadoresDinamicos) {
            m.remove();
        }
        marcadoresDinamicos.clear();
        Toast.makeText(this, "Mapa limpiado", Toast.LENGTH_SHORT).show();
    }

    private BitmapDescriptor crearIconoBitmap(int resId, int ancho, int alto) {
        Drawable drawable = ContextCompat.getDrawable(this, resId);
        if (drawable instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) drawable).getBitmap();
            Bitmap scaled = Bitmap.createScaledBitmap(b, ancho, alto, false);
            return BitmapDescriptorFactory.fromBitmap(scaled);
        }
        return BitmapDescriptorFactory.defaultMarker();
    }
}