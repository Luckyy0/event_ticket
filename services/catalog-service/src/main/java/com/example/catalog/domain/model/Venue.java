package com.example.catalog.domain.model;

public class Venue {
    private final String name;
    private final String address;
    private final String city;
    private final int capacity;

    public Venue(String name, String address, String city, int capacity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Venue name is required");
        }
        this.name = name.trim();
        this.address = address;
        this.city = city;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public int getCapacity() {
        return capacity;
    }
}
