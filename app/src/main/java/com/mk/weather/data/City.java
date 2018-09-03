package com.mk.weather.data;

import com.j256.ormlite.field.DatabaseField;

public class City {

    @DatabaseField(generatedId =  true)
    private int id;

    @DatabaseField(index = true)
    private String name;

    public City() {

    }

    public City(String city) {
        this.name = city;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
