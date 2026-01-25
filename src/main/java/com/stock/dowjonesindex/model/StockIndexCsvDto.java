package com.stock.dowjonesindex.model;
import com.opencsv.bean.CsvBindByPosition;

public class StockIndexCsvDto {
    @CsvBindByPosition(position = 0)
    private int quarter;

    @CsvBindByPosition(position = 1)
    private String stock;
    @CsvBindByPosition(position = 2)
    private String date;
    @CsvBindByPosition(position = 3)
    private String open;

    @CsvBindByPosition(position = 4)
    private String high;

    @CsvBindByPosition(position = 5)
    private String low;

    @CsvBindByPosition(position = 6)
    private String close;

    @CsvBindByPosition(position = 7)
    private String volume;

    @CsvBindByPosition(position = 8)
    private String percent_change_price;

    @CsvBindByPosition(position = 9)
    private String percent_change_volume_over_last_wk;

    @CsvBindByPosition(position = 10)
    private String previous_weeks_volume;

    @CsvBindByPosition(position = 11)
    private String next_weeks_open;

    @CsvBindByPosition(position = 12)
    private String next_weeks_close;

    @CsvBindByPosition(position = 13)
    private String percent_change_next_weeks_price;

    @CsvBindByPosition(position = 14)
    private String days_to_next_dividend;

    @CsvBindByPosition(position = 15)
    private String percent_return_next_dividend;

    public int getQuarter() {
        return quarter;
    }

    public void setQuarter(int quarter) {
        this.quarter = quarter;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getOpen() {
        return open;
    }

    public void setOpen(String open) {
        this.open = open;
    }

    public String getHigh() {
        return high;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    public String getLow() {
        return low;
    }

    public void setLow(String low) {
        this.low = low;
    }

    public String getClose() {
        return close;
    }

    public void setClose(String close) {
        this.close = close;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getPercent_change_price() {
        return percent_change_price;
    }

    public void setPercent_change_price(String percent_change_price) {
        this.percent_change_price = percent_change_price;
    }

    public String getPercent_change_volume_over_last_wk() {
        return percent_change_volume_over_last_wk;
    }

    public void setPercent_change_volume_over_last_wk(String percent_change_volume_over_last_wk) {
        this.percent_change_volume_over_last_wk = percent_change_volume_over_last_wk;
    }

    public String getPrevious_weeks_volume() {
        return previous_weeks_volume;
    }

    public void setPrevious_weeks_volume(String previous_weeks_volume) {
        this.previous_weeks_volume = previous_weeks_volume;
    }

    public String getNext_weeks_open() {
        return next_weeks_open;
    }

    public void setNext_weeks_open(String next_weeks_open) {
        this.next_weeks_open = next_weeks_open;
    }

    public String getNext_weeks_close() {
        return next_weeks_close;
    }

    public void setNext_weeks_close(String next_weeks_close) {
        this.next_weeks_close = next_weeks_close;
    }

    public String getPercent_change_next_weeks_price() {
        return percent_change_next_weeks_price;
    }

    public void setPercent_change_next_weeks_price(String percent_change_next_weeks_price) {
        this.percent_change_next_weeks_price = percent_change_next_weeks_price;
    }

    public String getDays_to_next_dividend() {
        return days_to_next_dividend;
    }

    public void setDays_to_next_dividend(String days_to_next_dividend) {
        this.days_to_next_dividend = days_to_next_dividend;
    }

    public String getPercent_return_next_dividend() {
        return percent_return_next_dividend;
    }

    public void setPercent_return_next_dividend(String percent_return_next_dividend) {
        this.percent_return_next_dividend = percent_return_next_dividend;
    }
}
