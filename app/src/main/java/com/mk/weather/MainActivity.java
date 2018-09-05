package com.mk.weather;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.mk.weather.data.City;
import com.mk.weather.data.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

public class MainActivity extends OrmLiteBaseActivity<DatabaseHelper> implements GetRawData.OnDownloadComplete {

    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    EditText cityName;
    TextView textView;
    Spinner spinner;
    ArrayAdapter<City> adapter;
    List<City> existingCities;
    String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: starts");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.my_toolbar);
        cityName = findViewById(R.id.cityName);
        textView = findViewById(R.id.textView);

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

    public  void okButton(View view) {
        city = cityName.getText().toString();
        loadCity();
        //Toast.makeText(getBaseContext(), city, Toast.LENGTH_SHORT).show();

    }

    public void loadCity() {
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
                    //dao.deleteById(3);
                    existingCities.add(newCity);
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
            Toast.makeText(getBaseContext(), "There is no city '" + city +"' in the base", Toast.LENGTH_SHORT).show();
        }
    }

}
