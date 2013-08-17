package com.ewaterhouse.getit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


public class FullscreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fullscreen_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_clear:
                clearPreferences();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearPreferences() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    public void onButtonClick(View v) {
        if(v.getId() == R.id.dummy_button){
            getZip();
        }
    }

    private void getZip() {
        Location loc = getCurrentLocation();
        double lat; double lon;
        if (loc == null) {
            lat = 37.423;
            lon = -122.086;
        } else {
            lat = loc.getLatitude();
            lon = loc.getLongitude();
        }

        String url = String.format("http://maps.googleapis.com/maps/api/geocode/json?latlng=%f,%f&sensor=false", lat, lon);
        SharedPreferences prefs = this.getPreferences(Context.MODE_PRIVATE);
        String res = prefs.getString(url, null);
        if (res != null) {
            updateContentView(res);
            return;
        }
        new HttpTask().execute(url);
    }

    private void updateContentView(String data) {
        final TextView contentView = (TextView)findViewById(R.id.fullscreen_content);
        contentView.setText(data);
    }

    Location getCurrentLocation() {
        LocationManager locationManager =  (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        Location newestLocation = null;
        List<String> providers = locationManager.getProviders(true);
        if (providers != null) {
            for (String provider : providers) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    if (newestLocation == null) {
                        newestLocation = location;
                    } else {
                        if (location.getTime() > newestLocation.getTime()) {
                            newestLocation = location;
                        }
                    }
                    //locationManager.requestLocationUpdates(provider, 0, 0, this);
                }
            }
        } else {
//            LocationDialogFragment dialog = new LocationDialogFragment();
//            dialog.show(getSupportFragmentManager(),
//                    LocationDialogFragment.class.getName());
            //newestLocation = locationManager.
        }
        return newestLocation;

    }


    private class HttpTask extends AsyncTask<String, Void, String> {
        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                String url= urls[0];

                StringBuilder stringBuilder = new StringBuilder();
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                try {
                    HttpResponse response = httpClient.execute(httpGet);
                    StatusLine statusLine = response.getStatusLine();
                    int statusCode = statusLine.getStatusCode();
                    if (statusCode == 200) {
                        HttpEntity entity = response.getEntity();
                        InputStream inputStream = entity.getContent();
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(inputStream));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        inputStream.close();
                    } else {
                        stringBuilder.append("failed dload");
                    }
                } catch (Exception e) {
                    stringBuilder.append(e.getLocalizedMessage());
                }

                JSONTokener tokener = new JSONTokener(stringBuilder.toString());
                JSONObject json = new JSONObject(tokener);
                String result = json.getJSONArray("results").getJSONObject(0).getString("formatted_address");

                SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                if (prefs.getAll().size() > 10) {
                    editor.remove(prefs.getAll().keySet().toArray()[0].toString());
                }
                editor.putString(url, result);
                editor.commit();

                return result;

            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(String result) {
            try {
                updateContentView(result);
            } catch (Exception e) {
                updateContentView(e.getLocalizedMessage());
            }
        }

    }
}
