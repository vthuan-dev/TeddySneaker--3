package com.web.application.entity;

import java.sql.Timestamp;

import com.web.application.dto.StatisticDTO;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SqlResultSetMappings(value = {
		@SqlResultSetMapping(name = "statisticDTO", classes = @ConstructorResult(targetClass = StatisticDTO.class, columns = {
				@ColumnResult(name = "sales", type = Long.class), @ColumnResult(name = "profit", type = Long.class),
				@ColumnResult(name = "quantity", type = Integer.class),
				@ColumnResult(name = "createdAt", type = String.class) })) })

@NamedNativeQuery(name = "getStatistic30Day", resultSetMapping = "statisticDTO", query = "SELECT s.sales, s.profit, s.quantity, FORMAT(s.created_at, 'yyyy-MM-dd') as createdAt "
		+ "FROM statistic s " + "WHERE s.created_at BETWEEN DATEADD(DAY, -30, GETDATE()) AND GETDATE() "
		+ "ORDER BY createdAt ASC")

@NamedNativeQuery(name = "getStatisticDayByDay", resultSetMapping = "statisticDTO", query = "SELECT s.sales, s.profit, s.quantity, FORMAT(s.created_at, 'yyyy-MM-dd') as createdAt "
		+ "FROM statistic s " + "WHERE s.created_at >= ?1 AND s.created_at <= ?2 " + "ORDER BY createdAt ASC")

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "statistic")
public class Statistic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	@Column(name = "sales")
	private long sales;
	@Column(name = "profit")
	private long profit;
	@Column(name = "quantity")
	private int quantity;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private Order order;
	@Column(name = "created_at")
	private Timestamp createdAt;
}
