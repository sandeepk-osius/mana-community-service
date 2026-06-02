package com.manacommunity.api.dto;

import lombok.Data;

/**
 * DTO for creating or updating a Venue.
 */
@Data
public class VenueRequest {

    private String name;

    private String address;

    private String city;

    private String area;

    private String pinCode;

    private String mapLink;

    private Integer capacity;

    private String venueType; // APARTMENT, COLLEGE, SCHOOL, OFFICE, CLUB, OUTSIDE

    private String venueCategory; // Community name or SPORTS_VENUE, PUBLIC_PARK, etc.

    private String openingTime; // e.g. "08:00 AM"

    private String closingTime; // e.g. "08:00 PM"

    private String contactName;

    private String contactNumber;

    private String contactEmail;
}
