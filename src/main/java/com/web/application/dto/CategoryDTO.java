package com.web.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CategoryDTO {
    private long id;
    private String name;
    private boolean status;
    private Timestamp createdAt;
}
