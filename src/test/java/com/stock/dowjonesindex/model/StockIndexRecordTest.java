package com.stock.dowjonesindex.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockIndexRecordTest {

    @Test
    void gettersAndSetters_roundTrip() {
        StockIndexRecord record = new StockIndexRecord();

        record.setId(1L);
        record.setQuarter(2);
        record.setStock("AA");
        record.setDate(LocalDate.of(2011, 1, 14));
        record.setOpen(10.0);
        record.setHigh(11.0);
        record.setLow(9.0);
        record.setClose(10.5);
        record.setVolume(123L);

        assertEquals(1L, record.getId());
        assertEquals(2, record.getQuarter());
        assertEquals("AA", record.getStock());
        assertEquals(LocalDate.of(2011, 1, 14), record.getDate());
        assertEquals(10.0, record.getOpen());
        assertEquals(11.0, record.getHigh());
        assertEquals(9.0, record.getLow());
        assertEquals(10.5, record.getClose());
        assertEquals(123L, record.getVolume());
    }

    @Test
    void equalsAndHashCode_sameValues_areEqual() {
        StockIndexRecord a = new StockIndexRecord();
        a.setId(1L);
        a.setQuarter(1);
        a.setStock("AA");
        a.setDate(LocalDate.of(2011, 1, 14));
        a.setOpen(1.0);
        a.setHigh(2.0);
        a.setLow(0.5);
        a.setClose(1.5);
        a.setVolume(100L);
        a.setPercentChangePrice(1.2);
        a.setPercentChangeVolumeOverLastWk(2.3);
        a.setPreviousWeeksVolume(200.0);
        a.setNextWeeksOpen(1.1);
        a.setNextWeeksClose(1.2);
        a.setPercentChangeNextWeeksPrice(0.5);
        a.setDaysToNextDividend(10);
        a.setPercentReturnNextDividend(0.2);

        StockIndexRecord b = new StockIndexRecord();
        b.setId(1L);
        b.setQuarter(1);
        b.setStock("AA");
        b.setDate(LocalDate.of(2011, 1, 14));
        b.setOpen(1.0);
        b.setHigh(2.0);
        b.setLow(0.5);
        b.setClose(1.5);
        b.setVolume(100L);
        b.setPercentChangePrice(1.2);
        b.setPercentChangeVolumeOverLastWk(2.3);
        b.setPreviousWeeksVolume(200.0);
        b.setNextWeeksOpen(1.1);
        b.setNextWeeksClose(1.2);
        b.setPercentChangeNextWeeksPrice(0.5);
        b.setDaysToNextDividend(10);
        b.setPercentReturnNextDividend(0.2);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void jpaAnnotations_entityTableAndUniqueConstraint_present() {
        assertNotNull(StockIndexRecord.class.getAnnotation(Entity.class));

        Table table = StockIndexRecord.class.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("stock_data", table.name());

        UniqueConstraint[] constraints = table.uniqueConstraints();
        assertTrue(constraints.length >= 1);

        boolean hasStockDateUnique = Arrays.stream(constraints)
                .anyMatch(c -> Arrays.equals(new String[]{"stock", "date"}, c.columnNames()));
        assertTrue(hasStockDateUnique);
    }
}

