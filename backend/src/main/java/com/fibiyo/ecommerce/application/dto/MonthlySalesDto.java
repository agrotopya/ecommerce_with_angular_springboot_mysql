package com.fibiyo.ecommerce.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class MonthlySalesDto {
    private String monthYear; // Format: "YYYY-MM" veya "MonthName YYYY"
    private BigDecimal totalSales;

    // JPQL sorgusundan gelen tiplere uygun constructor
    public MonthlySalesDto(String monthYear, Number totalSales) {
        this.monthYear = monthYear;
        this.totalSales = (totalSales != null) ? BigDecimal.valueOf(totalSales.doubleValue()) : BigDecimal.ZERO;
    }

    // Lombok @AllArgsConstructor'ın yerini alacak tam constructor (eğer başka yerde kullanılıyorsa)
    public MonthlySalesDto(String monthYear, BigDecimal totalSales) {
        this.monthYear = monthYear;
        this.totalSales = totalSales;
    }
}
