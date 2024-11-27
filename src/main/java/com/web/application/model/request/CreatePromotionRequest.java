package com.web.application.model.request;

import java.sql.Timestamp;
import java.util.ArrayList;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreatePromotionRequest {
	@NotBlank(message = "Mã code rỗng")
	@Pattern(regexp = "^[0-9A-Z-]+$", message = "Mã code không đúng định dạng")
	@Size(min = 1, max = 10, message = "Độ dài tên từ 1 - 10 kí tự")
	private String code;

	@NotBlank(message = "Tên rỗng")
	@Size(min = 1, max = 300, message = "Độ dài tên từ 1 - 300 kí tự")
	private String name;

	@Min(1)
	@Max(2)
	@JsonProperty("discount_type")
	private int discountType;

	@JsonProperty("discount_value")
	private long discountValue;

	@JsonProperty("max_value")
	private long maxValue;

	@JsonProperty("is_public")
	private boolean isPublic;
	
	@JsonProperty("user_ids")
	private ArrayList<Integer> userIds;
	
	private boolean active;

	@JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
	@JsonProperty("expired_date")
	private Timestamp expiredDate;
}
