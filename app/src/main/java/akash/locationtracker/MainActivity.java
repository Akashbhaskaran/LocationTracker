package akash.locationtracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends ActionBarActivity
    implements GoogleApiClient.ConnectionCallbacks,
               GoogleApiClient.OnConnectionFailedListener,
               LocationListener {

    Location mCurrentLocation;
    String mLastUpdateTime;
    Button reset,overview;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    GoogleMap googleMap;

    private static final long INTERVAL = 1000 * 60 * 1; //1 minute
    private static final long FASTEST_INTERVAL = 1000 * 60 * 1; // 1 minute
    private static final String TAG = "LocationTracker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logEvent("onCreate");

        //show error dialog if GoolglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        // Create Location Request
        createLocationRequest();
        // Initialize GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        setContentView(R.layout.activity_main);
        initialize();

         reset = (Button)findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SQLiteDatabase database = new SQLiteDBHelper(getApplicationContext()).getWritableDatabase();
                database.delete(SQLiteDBHelper.TABLE_NAME,null,null);
            }
        });

        overview = (Button)findViewById(R.id.overview);
        overview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SQLiteDatabase database = new SQLiteDBHelper(getApplicationContext()).getWritableDatabase();
                String[] projection = {
                        SQLiteDBHelper.COLUMN_ID,
                        SQLiteDBHelper.COLUMN_LAT,
                        SQLiteDBHelper.COLUMN_LONG,
                        SQLiteDBHelper.COLUMN_TIME
                };
                Cursor cursor = database.query(
                        SQLiteDBHelper.TABLE_NAME,   // The table to query
                        projection,                               // The columns to return
                        null,                                // The columns for the WHERE clause
                        null,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        null                                      // don't sort
                );

                Log.d("TAG", "The total cursor count is " + cursor.getCount());
                ArrayList<LatLng> points = new ArrayList<LatLng>();
                Polyline line;
                try {
                    while (cursor.moveToNext()) {
                        PolylineOptions polyLine = new PolylineOptions().color(
                                Color.BLUE).width((float) 7.0);
                        double lati = Double.parseDouble(cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_LAT)));
                        double lngi = Double.parseDouble(cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_LONG)));
                        Log.d("TAG", "The cursor  is long" +cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_LONG) ));
                        Log.d("TAG", "The cursor  is lat" +cursor.getString(cursor.getColumnIndex(SQLiteDBHelper.COLUMN_LAT) ));

                        LatLng latLng = new LatLng(lati,lngi);
                        points.add(latLng);

                    }
                } finally {
                    cursor.close();
                }
                googleMap.clear();  //clears all Markers and Polylines

                PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                for (int i = 0; i < points.size(); i++) {
                    LatLng point = points.get(i);
                    options.add(point);
                }
                addMarker(); //add Marker in current position
                line = googleMap.addPolyline(options);

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        logEvent("onStart");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mGoogleApiClient.isConnected()) {
            startLocationUpdates();
            logEvent("Location update resumed");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
       // stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        logEvent("onStop");
        mGoogleApiClient.disconnect();
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    private void initialize() {
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        googleMap = fm.getMap();
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * LocationRequest objects are used to request of service parameters
     * for request to the FusedLocationProviderApi.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    public void onLocationChanged(Location location) {
        logEvent("onLocationChanged");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        addMarker();
        SQLiteDatabase database = new SQLiteDBHelper(this).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SQLiteDBHelper.COLUMN_LAT, mCurrentLocation.getLatitude());
        values.put(SQLiteDBHelper.COLUMN_LONG, mCurrentLocation.getLongitude());
        values.put(SQLiteDBHelper.COLUMN_TIME, mLastUpdateTime.toString());
        long newRowId = database.insert(SQLiteDBHelper.TABLE_NAME, null, values);
         values = new ContentValues();
        values.put(SQLiteDBHelper.COLUMN_LAT, mCurrentLocation.getLatitude()+1.4);
        values.put(SQLiteDBHelper.COLUMN_LONG, mCurrentLocation.getLongitude()+1.4);
        values.put(SQLiteDBHelper.COLUMN_TIME, mLastUpdateTime.toString());
         newRowId = database.insert(SQLiteDBHelper.TABLE_NAME, null, values);


    }

    private void addMarker() {
        logEvent("addMarker");
        MarkerOptions options = new MarkerOptions();
        // To display the time as title for location markers
        IconGenerator iconFactory = new IconGenerator(this);
        iconFactory.setStyle(IconGenerator.STYLE_BLUE);
        options.icon(BitmapDescriptorFactory.
                fromBitmap(iconFactory.makeIcon(mLastUpdateTime)));
        options.anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        // Set marker
        LatLng currentLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        options.position(currentLatLng);
        Marker marker = googleMap.addMarker(options);
        long atTime = mCurrentLocation.getTime();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date(atTime));
        marker.setTitle(mLastUpdateTime);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13));
        logEvent("Marker added");
    }

    /**
     * Before any operation is executed, the GoogleApiClient must be connected using the connect() method.
     * The client is not considered connected until the onConnected(Bundle) callback has been called.
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        logEvent("onConnected");
        startLocationUpdates();
    }

    /**
     * Request location updates with GoogleApiClient object, LocationRequest and
     * listener to pass result.
     */
    private void startLocationUpdates() {
        logEvent("startLocationUpdates");
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.
                requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * User is leaving screen, so stop location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        logEvent("Location update stopped.");
    }

    /**
     * On connection suspended with Google API client, this method will be called.
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
    }

    /**
     * On a failure to connect Google API client, this method will be called.
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        logEvent("Connection failed: " + connectionResult.toString());
    }

    /**
     * Logs event
     * @param event
     */
    private void logEvent(String event) {
        Log.d(TAG, event);
    }
}