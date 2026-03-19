package com.hub.conges.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NagerDateHolidayDto {
    private LocalDate date;
    private String localName;
    private String name;
}
