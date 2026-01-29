package com.stock.dowjonesindex.dto;

import com.stock.dowjonesindex.validation.ValidDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StockIndexUpdateRequest(
        @NotBlank(message = "quarter is required")
        @Pattern(regexp = "^[1-6]$", message = "quarter must be integer between 1 and 6")
        String quarter,

        @NotBlank(message = "stock is required")
        String stock,

        @NotBlank(message = "date is required")
        @ValidDate(pattern = "M/dd/yyyy", message = "date must be in M/dd/yyyy format (example: 1/05/2015)")
        String date,

        @NotBlank(message = "open is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "open must be a valid number value between 0-9")
        String open,

        @NotBlank(message = "high is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "high must be a valid number value between 0-9")
        String high,

        @NotBlank(message = "low is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "low must be a valid number value between 0-9")
        String low,

        @NotBlank(message = "close is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "close must be a valid number value between 0-9")
        String close,

        @NotBlank(message = "volume is required")
        @Pattern(regexp = "^[1-9]\\d*$", message = "volume must be a valid number value between 0-9")
        String volume
) {}
