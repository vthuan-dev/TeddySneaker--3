package com.web.application.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.web.application.dto.StatisticDTO;
import com.web.application.entity.Statistic;

@Repository
public interface StatisticRepository extends JpaRepository<Statistic, Long> {

    @Query(value = "SELECT s.created_at as createdAt, SUM(s.quantity) as quantity, SUM(s.sales) as sales, SUM(s.profit) as profit " +
            "FROM statistic s " +
            "WHERE s.created_at >= DATEADD(day, -30, GETDATE()) " +
            "GROUP BY s.created_at " +
            "ORDER BY s.created_at ASC", nativeQuery = true)
    List<StatisticDTO> getStatistic30Day();

    @Query(value = "SELECT s.created_at as createdAt, SUM(s.quantity) as quantity, SUM(s.sales) as sales, SUM(s.profit) as profit " +
            "FROM statistic s " +
            "WHERE s.created_at >= CAST(:fromDate AS DATE) AND s.created_at <= CAST(:toDate AS DATE) " +
            "GROUP BY s.created_at " +
            "ORDER BY s.created_at ASC", nativeQuery = true)
    List<StatisticDTO> getStatisticDayByDay(String toDate, String fromDate);

    @Query(value = "SELECT * FROM statistic WHERE CONVERT(date, created_at) = CONVERT(date, GETDATE())", nativeQuery = true)
    Statistic findByCreatedAT();
}
