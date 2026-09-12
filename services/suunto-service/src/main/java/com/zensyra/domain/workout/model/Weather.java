package com.zensyra.domain.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Weather {

    @Column(name = "weather_humidity")
    public Double humidity;

    @Column(name = "weather_wind_speed")
    public Double windSpeed;

    @Column(name = "weather_temperature")
    public Double temperature;

    @Column(name = "weather_icon")
    public String weatherIcon;

    @Column(name = "weather_wind_direction")
    public Double windDirection;
}