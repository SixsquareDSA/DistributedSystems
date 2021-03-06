package gr.aueb.cs.ds.dsapp.getpoi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.w3c.dom.Text;

import java.util.ArrayList;

import gr.aueb.cs.ds.dsapp.MainActivity;
import gr.aueb.cs.ds.dsapp.R;

public class SelectArea extends Activity implements GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMarkerDragListener{

    private GoogleMap googleMap;
    private Polygon polygon;
    private Marker point1;
    private Marker point2;

    private ArrayList<Button> buttons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_area);

        googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        MapsInitializer.initialize(this);
        googleMap.setOnInfoWindowClickListener(this);
        googleMap.setOnMarkerDragListener(this);
        onMapReady(googleMap);

        setUpButtons();
    }

    @Override
    public boolean onKeyUp(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                popUp();
                return true;
        }
        //System.out.println(keycode);
        //return true;
        return super.onKeyUp(keycode, e);
    }

    private void popUp() {
        LinearLayout layout = new LinearLayout(getBaseContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final TextView lltitle = new TextView(getBaseContext());
        lltitle.setText("Lower Left Point");
        final EditText llpoint = new EditText(getBaseContext());
        layout.addView(lltitle);
        layout.addView(llpoint);

        final TextView trtitle = new TextView(getBaseContext());
        trtitle.setText("Top Right Point");
        final EditText trpoint = new EditText(getBaseContext());
        layout.addView(trtitle);
        layout.addView(trpoint);

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Manual Insert Area")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        System.out.println(llpoint.getText());
                        System.out.println(trpoint.getText());
                        try {
                            String[] llpointL = llpoint.getText().toString().split(",");
                            String[] trpointL = trpoint.getText().toString().split(",");
                            LatLng loc1 = new LatLng(Float.parseFloat(llpointL[0]), Float.parseFloat(llpointL[1]));
                            LatLng loc2 = new LatLng(Float.parseFloat(trpointL[0]), Float.parseFloat(trpointL[1]));
                            point1.setPosition(loc1);
                            point2.setPosition(loc2);
                            highlightArea();
                        } catch (Exception fu) {
                            System.out.println("Bad input");
                        }

                    }
                });

        alert.setView(layout);
        alert.show();
    }

    private void setUpButtons() {
        buttons = new ArrayList<Button>();

        Button next = (Button) findViewById(R.id.next_button);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                nextActivity();
            }
        });

        Button prev = (Button) findViewById(R.id.cancel_button);
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                previousActivity();
            }
        });

        buttons.add(next);
        buttons.add(prev);
    }

    private void nextActivity() {
        Intent intent = new Intent(this, SelectTime.class);
        intent.putExtra("Point1", point1.getPosition());
        intent.putExtra("Point2", point2.getPosition());
        startActivity(intent);
    }

    private void previousActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void onInfoWindowClick(Marker marker) {
        System.out.println("InfoWindowClicked");
    }

    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setRotateGesturesEnabled(false);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String[] llpoint = SP.getString("llp", "40.55,-75.0").split(",");
        String[] trpoint = SP.getString("trp", "40.99,-73.0").split(",");
        LatLng def1 = new LatLng(Float.parseFloat(llpoint[0]), Float.parseFloat(llpoint[1]));
        LatLng def2 = new LatLng(Float.parseFloat(trpoint[0]), Float.parseFloat(trpoint[1]));

        LatLng newYork = new LatLng(40.7127784,-74.0409606);
        point1 = googleMap.addMarker(
                new MarkerOptions().position(def1).title("Point 1").flat(true).draggable(true));
        point2 = googleMap.addMarker(
                new MarkerOptions().position(def2).title("Point 2").flat(true).draggable(true));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newYork, 9));

        highlightArea();
    }

    private void highlightArea() {
        LatLng corner1 = point1.getPosition();
        LatLng corner2 = new LatLng(point1.getPosition().latitude, point2.getPosition().longitude);
        LatLng corner3 = point2.getPosition();
        LatLng corner4 = new LatLng(point2.getPosition().latitude, point1.getPosition().longitude);

        if (polygon!=null) {
            polygon.remove();
        }
        PolygonOptions options = new PolygonOptions();
        options.add(corner1, corner2, corner3, corner4);
        options.strokeWidth(5);
        options.fillColor(Color.argb(155, 255, 0, 0));

        polygon = googleMap.addPolygon(options);
    }

    public boolean onMarkerClick(Marker arg0) {
        return true;
    }

    public void onMarkerDrag(Marker marker) {
        highlightArea();
    }

    public void onMarkerDragStart(Marker marker) {

    }

    public void onMarkerDragEnd(Marker marker) {
        highlightArea();
    }

}
