package com.stock.dowjonesindex.repository;

import com.stock.dowjonesindex.model.StockIndexRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<StockIndexRecord, Long> {

    // 🔹 SELECT BY ID (already exists in JpaRepository)
    Optional<StockIndexRecord> findById(Long id);
    // 🔹 DELETE BY ID (already exists)
    void deleteById(Long id);
    // 🔹 EXISTS BY ID
    boolean existsById(Long id);

    // 🔹 OPTIONAL: FIND BY STOCK NAME
    List<StockIndexRecord> findByStock(String stock);
    @Transactional
    @Modifying
    @Query("""
UPDATE StockIndexRecord s
        SET s.quarter = :quarter,
            s.stock = :stock,
            s.date=:date,
            s.open = :open,
            s.high = :high,
            s.low = :low,
            s.close = :close,
            s.volume = :volume
        WHERE s.id = :id""")
    int updateById(@Param("id") Long id,
                   @Param("quarter") Integer quarter,
                   @Param("stock") String stock,
                   @Param("date") LocalDate date,
                   @Param("open") Double open,
                   @Param("high") Double high,
                   @Param("low") Double low,
                   @Param("close") Double close,
                   @Param("volume") Long volume);

}
