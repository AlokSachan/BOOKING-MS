package com.wipro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyDto {

    private Integer id;
    private String title;
    private String location;
    private String type;
    private Double price;
    private Integer availableRooms;
    private Boolean lockForBook;
}