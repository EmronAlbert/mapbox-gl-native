package com.mapbox.mapboxsdk.testapp.activity.geocoding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mapbox.geocoder.GeocoderCriteria;
import com.mapbox.geocoder.MapboxGeocoder;
import com.mapbox.geocoder.service.models.GeocoderFeature;
import com.mapbox.geocoder.service.models.GeocoderResponse;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Projection;
import com.mapbox.mapboxsdk.testapp.R;
import com.mapbox.mapboxsdk.testapp.model.annotations.CountryMarkerView;
import com.mapbox.mapboxsdk.testapp.model.annotations.PulseMarkerView;
import com.mapbox.mapboxsdk.testapp.model.annotations.PulseMarkerViewOptions;

import java.util.List;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class GeocoderActivity extends AppCompatActivity {

    private static final String LOG_TAG = "GeocoderActivity";

    private MapView mapView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geocoder);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        textView = (TextView) findViewById(R.id.message);
        setMessage(getString(R.string.geocoder_instructions));

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setStyleUrl(Style.MAPBOX_STREETS);
        mapView.onCreate(savedInstanceState);

        final ImageView dropPinView = new ImageView(this);
        dropPinView.setImageResource(R.drawable.ic_add_24dp);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        dropPinView.setLayoutParams(params);
        mapView.addView(dropPinView);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                final Projection projection = mapboxMap.getProjection();
                final int width = mapView.getMeasuredWidth();
                final int height = mapView.getMeasuredHeight();

                // Click listener
                mapboxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng point) {
                        PointF centerPoint = new PointF(width / 2, (height + dropPinView.getHeight()) / 2);
                        LatLng centerLatLng = new LatLng(projection.fromScreenLocation(centerPoint));

                        setMessage("Geocoding...");

                        mapboxMap.removeAnnotations();
                        mapboxMap.addMarker(new MarkerOptions().position(centerLatLng));

                        geocode(centerLatLng);
                    }
                });

                // Add MarkerView
                final LatLng markerPosition = new LatLng(38.907298, -77.043478);
                final MarkerView markerView = mapboxMap.addMarker(new PulseMarkerViewOptions()
                        .anchor(0.5f, 0.5f)
                        .position(markerPosition)
                );

                final float circleDiameterSize = getResources().getDimension(R.dimen.circle_size);
                mapboxMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {

                    private Animation pulseAnimation;

                    @Override
                    public void onCameraChange(CameraPosition position) {
                        View convertView = mapboxMap.getMarkerViewManager().getView(markerView);
                        if (convertView != null) {
                            View backgroundView = convertView.findViewById(R.id.background_imageview);
                            if (pulseAnimation == null && position.target.distanceTo(markerPosition) < 0.5f * circleDiameterSize) {
                                pulseAnimation = AnimationUtils.loadAnimation(GeocoderActivity.this, R.anim.pulse);
                                pulseAnimation.setRepeatCount(Animation.INFINITE);
                                pulseAnimation.setRepeatMode(Animation.RESTART);
                                backgroundView.startAnimation(pulseAnimation);
                            } else if (pulseAnimation != null && position.target.distanceTo(markerPosition) >= 0.6f * circleDiameterSize) {
                                backgroundView.clearAnimation();
                                pulseAnimation = null;
                            }
                        }
                    }
                });

                mapboxMap.getMarkerViewManager().addMarkerViewAdapter(new PulseMarkerViewAdapter(GeocoderActivity.this));
            }
        });
    }


    private static class PulseMarkerViewAdapter extends MapboxMap.MarkerViewAdapter<PulseMarkerView> {

        private LayoutInflater inflater;

        public PulseMarkerViewAdapter(@NonNull Context context) {
            super(context);
            this.inflater = LayoutInflater.from(context);
        }

        @Nullable
        @Override
        public View getView(@NonNull PulseMarkerView marker, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = inflater.inflate(R.layout.view_pulse_marker, parent, false);
                viewHolder.foregroundImageView = (ImageView) convertView.findViewById(R.id.foreground_imageView);
                viewHolder.backgroundImageView = (ImageView) convertView.findViewById(R.id.background_imageview);
                convertView.setTag(viewHolder);
            }
            return convertView;
        }

        private static class ViewHolder {
            ImageView foregroundImageView;
            ImageView backgroundImageView;
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /*
     * Forward geocoding
     */

    private void geocode(final LatLng point) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                MapboxGeocoder client = new MapboxGeocoder.Builder()
                        .setAccessToken(getString(R.string.mapbox_access_token))
                        .setCoordinates(point.getLongitude(), point.getLatitude())
                        .setType(GeocoderCriteria.TYPE_POI)
                        .build();

                client.enqueue(new Callback<GeocoderResponse>()

                               {
                                   @Override
                                   public void onResponse(Response<GeocoderResponse> response, Retrofit retrofit) {
                                       List<GeocoderFeature> results = response.body().getFeatures();
                                       if (results.size() > 0) {
                                           String placeName = results.get(0).getPlaceName();
                                           setSuccess(placeName);
                                       } else {
                                           setMessage("No results.");
                                       }
                                   }

                                   @Override
                                   public void onFailure(Throwable t) {
                                       setError(t.getMessage());
                                   }
                               }

                );
                return null;
            }
        }.execute();
    }

    /*
     * Update text view
     */

    private void setMessage(String message) {
        Log.d(LOG_TAG, "Message: " + message);
        textView.setText(message);
    }

    private void setSuccess(String placeName) {
        Log.d(LOG_TAG, "Place name: " + placeName);
        textView.setText(placeName);
    }

    private void setError(String message) {
        Log.e(LOG_TAG, "Error: " + message);
        textView.setText("Error: " + message);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
