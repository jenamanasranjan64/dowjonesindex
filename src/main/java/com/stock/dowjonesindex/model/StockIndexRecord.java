package com.stock.dowjonesindex.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "stock_data",uniqueConstraints = {@UniqueConstraint(columnNames = {"stock", "date"})})
@Data
public class StockIndexRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int quarter;
    private String stock; // e.g., AA
    @JsonFormat(pattern = "M/d/yyyy")
    private LocalDate date;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private long volume;
    private Double percentChangePrice;
    private Double percentChangeVolumeOverLastWk;
    private Double previousWeeksVolume;
    private Double nextWeeksOpen;
    private Double nextWeeksClose;
    private Double percentChangeNextWeeksPrice;
    private int daysToNextDividend;
    private Double percentReturnNextDividend;
}
