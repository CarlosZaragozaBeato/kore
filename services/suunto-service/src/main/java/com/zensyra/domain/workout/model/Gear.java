package com.zensyra.domain.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Gear {

    @Column(name = "gear_name")
    public String name;

    @Column(name = "gear_display_name")
    public String displayName;

    @Column(name = "gear_product_type")
    public String productType;

    @Column(name = "gear_manufacturer")
    public String manufacturer;

    @Column(name = "gear_serial_number")
    public String serialNumber;

    @Column(name = "gear_hardware_version")
    public String hardwareVersion;

    @Column(name = "gear_software_version")
    public String softwareVersion;
}