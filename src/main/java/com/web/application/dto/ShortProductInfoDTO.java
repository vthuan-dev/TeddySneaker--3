package com.web.application.dto;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ShortProductInfoDTO {
	private String id;

	private String name;

	private long price;

	List<Integer> availableSizes;

	public ShortProductInfoDTO(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public ShortProductInfoDTO(String id, String name, long price, Object availableSizes) {
		this.id = id;
		this.name = name;
		this.price = price;
		if (availableSizes != null) {
			try {
				String sizesStr = (String) availableSizes;
				this.availableSizes = Arrays.stream(sizesStr.split(",")).map(String::trim).map(Integer::parseInt)
						.collect(Collectors.toList());
			} catch (Exception e) {
				this.availableSizes = null;
			}
		}
	}
}