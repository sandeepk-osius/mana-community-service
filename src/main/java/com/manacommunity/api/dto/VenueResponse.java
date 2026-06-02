package com.manacommunity.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO returned by the Venue REST API.
 * Flattens the community relationship into a simple communityId + communityName.
 */
@Data
@Builder
public class VenueResponse {

    private Long id;

    private String name;

    private String address;

    private String city;

    private String area;

    private String pinCode;

    private String mapLink;

    private Integer capacity;

    private String venueType;

    private String venueCategory;

    private String openingTime;

    private String closingTime;

    private String contactName;

    private String contactNumber;

    private String contactEmail;

    private Long communityId;

    private String communityName;
}
