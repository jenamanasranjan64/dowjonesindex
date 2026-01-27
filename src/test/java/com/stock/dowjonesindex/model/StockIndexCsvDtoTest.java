package com.stock.dowjonesindex.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockIndexCsvDtoTest {

    @Test
    void settersAndGetters_roundTrip() {
        StockIndexCsvDto dto = new StockIndexCsvDto();
        dto.setQuarter(3);
        dto.setStock("AA");
        dto.setDate("1/05/2015");
        dto.setOpen("10.1");
        dto.setHigh("11.2");
        dto.setLow("9.9");
        dto.setClose("10.0");
        dto.setVolume("123");
        dto.setPercent_change_price("1.0");
        dto.setPercent_change_volume_over_last_wk("2.0");
        dto.setPrevious_weeks_volume("100");
        dto.setNext_weeks_open("12.0");
        dto.setNext_weeks_close("13.0");
        dto.setPercent_change_next_weeks_price("3.0");
        dto.setDays_to_next_dividend("7");
        dto.setPercent_return_next_dividend("0.1");

        assertEquals(3, dto.getQuarter());
        assertEquals("AA", dto.getStock());
        assertEquals("1/05/2015", dto.getDate());
        assertEquals("10.1", dto.getOpen());
        assertEquals("11.2", dto.getHigh());
        assertEquals("9.9", dto.getLow());
        assertEquals("10.0", dto.getClose());
        assertEquals("123", dto.getVolume());
        assertEquals("1.0", dto.getPercent_change_price());
        assertEquals("2.0", dto.getPercent_change_volume_over_last_wk());
        assertEquals("100", dto.getPrevious_weeks_volume());
        assertEquals("12.0", dto.getNext_weeks_open());
        assertEquals("13.0", dto.getNext_weeks_close());
        assertEquals("3.0", dto.getPercent_change_next_weeks_price());
        assertEquals("7", dto.getDays_to_next_dividend());
        assertEquals("0.1", dto.getPercent_return_next_dividend());
    }
}

