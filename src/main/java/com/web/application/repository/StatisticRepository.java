package com.web.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.web.application.dto.StatisticDTO;
import com.web.application.entity.Statistic;

import java.util.List;

@Repository
public interface StatisticRepository extends JpaRepository<Statistic, Long> {

    @Query(name = "getStatistic30Day",nativeQuery = true)
    List<StatisticDTO> getStatistic30Day();

    @Query(name = "getStatisticDayByDay",nativeQuery = true)
    List<StatisticDTO> getStatisticDayByDay(String toDate, String formDate);

    @Query(value = "SELECT * FROM statistic WHERE FORMAT(created_at, 'yyyy-MM-dd') = FORMAT(GETDATE(), 'yyyy-MM-dd')", nativeQuery = true)
    Statistic findByCreatedAT();


}
