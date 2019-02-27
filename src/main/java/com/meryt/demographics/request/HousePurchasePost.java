package com.meryt.demographics.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HousePurchasePost {
    private Long newOwnerId;
    private String onDate;
}
