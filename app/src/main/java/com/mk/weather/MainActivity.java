package com.mk.weather;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.support.design.widget.Snackbar;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.mk.weather.data.City;
import com.mk.weather.data.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

public class MainActivity extends OrmLiteBaseActivity<DatabaseHelper>  implements GetRawData.OnDownloadComplete, AppCompatCallback {

    private AppCompatDelegate delegate;
    private static final String TAG = "MainActivity";
    ConstraintLayout constrLayout;
    private Toolbar toolbar;
    final Context context = this;
    TextView textView;
    Spinner spinner;
    ProgressBar progressBar;
    ArrayAdapter<City> adapter;
    List<City> existingCities;
    String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: starts");
        super.onCreate(savedInstanceState);
        setTitle(null);
        setContentView(R.layout.activity_main);

        delegate = AppCompatDelegate.create(this, this);
        delegate.onCreate(savedInstanceState);
        delegate.setContentView(R.layout.activity_main);

        constrLayout = findViewById(R.id.constrLayout);
        toolbar = findViewById(R.id.my_toolbar);
        delegate.setSupportActionBar(toolbar);
        textView = findViewById(R.id.textView);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.VISIBLE);

        try {
            Dao<City, Integer> dao = getHelper().getCityDao();
            existingCities = dao.queryForAll();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, existingCities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Log.d(TAG, spinner.getSelectedItem().toString());
                city = spinner.getSelectedItem().toString();
                loadCity();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                Log.d(TAG, "Nothing selected");
            }
        });

        Log.d(TAG, "onCreate: ends");

    }

    public  boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.city_menu, menu);
        return  true;

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        LayoutInflater nc = LayoutInflater.from(context);
        View newCityView = nc.inflate(R.layout.new_city, null);
        AlertDialog.Builder newCityDialogBuilder = new AlertDialog.Builder(context);
        newCityDialogBuilder.setView(newCityView);
        final EditText cityInput = newCityView.findViewById(R.id.input_city);
        newCityDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                city = cityInput.getText().toString();
                                loadCity();
                            }
                        })
                .setNegativeButton("Отмена",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog createDialog = newCityDialogBuilder.create();
        createDialog.show();
        return super.onOptionsItemSelected(item);

    }

    public void loadCity() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        GetRawData getRawData = new GetRawData(this);
        //getRawData.execute("https://api.openweathermap.org/data/2.5/weather?APPID=6e71959cff1c0c71a6049226d45c69a1&units=metric&id=2172797");
        getRawData.execute("https://api.openweathermap.org/data/2.5/weather?APPID=6e71959cff1c0c71a6049226d45c69a1&units=metric&q=" + city);

    }

    @Override
    public void onDownloadComplete(String data, DownloadStatus status) {
        if (status == DownloadStatus.OK) {
            Log.d(TAG, "onDownloadComplete: data is " + data);

            boolean alreadyExist = false;
            for (City thisCity : existingCities) {
                if (thisCity.getName().equals(city)) {
                    alreadyExist = true;
                }
            }
            if (!alreadyExist) {
                try {
                    Dao<City, Integer> dao = getHelper().getCityDao();
                    City newCity = new City(city);
                    dao.create(newCity);
                    existingCities.add(newCity);
                    int spinnerPosition = 0;
                    int i = 0;
                    for(City thisCity : existingCities) {
                        if(thisCity.getName().equals(city)) {
                            spinnerPosition = i;
                        }
                        i++;
                    }
                    spinner.setSelection(spinnerPosition);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            //Log.d(TAG, Integer.toString(existingCities.size()));

            try {
                JSONObject jObject = new JSONObject(data);
                JSONArray jArray = jObject.getJSONArray("weather");
                JSONObject descriptionObject = jArray.getJSONObject(0);
                String description = descriptionObject.getString("description");
                JSONObject mainObject = jObject.getJSONObject("main");
                double temp = mainObject.getDouble("temp");
                String temperature = Double.toString(temp);
                Log.d(TAG, "description: " + description + ", temp: " + temperature);
                StringBuilder sb = new StringBuilder();
                sb.append("description: ").append(description).append('\n');
                sb.append("temp: ").append(temperature);
                textView.setText(sb.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing data " + e.toString());
            }


        } else {
            // download or processing failed
            Log.e(TAG, "onDownloadComplete failed with status " + status);
            Snackbar.make(constrLayout, "There is no city '" + city +"' in the base", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            //Toast.makeText(getBaseContext(), "There is no city '" + city +"' in the base", Toast.LENGTH_SHORT).show();
        }
        progressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {

    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {

    }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
    }

}
