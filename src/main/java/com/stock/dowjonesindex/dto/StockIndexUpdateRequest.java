package com.stock.dowjonesindex.dto;

import com.stock.dowjonesindex.validation.ValidDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StockIndexUpdateRequest(
        @NotBlank(message = "quarter is required")
        @Pattern(regexp = "^[1-4]$", message = "quarter must be integer between 1 and 4")
        String quarter,

        @NotBlank(message = "stock is required")
        String stock,

        @NotBlank(message = "date is required")
        @ValidDate(pattern = "M/dd/yyyy", message = "date must be in M/dd/yyyy format (example: 1/05/2015)")
        String date,

        @NotBlank(message = "open is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "open must be a valid number >= 0")
        String open,

        @NotBlank(message = "high is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "high must be a valid number >= 0")
        String high,

        @NotBlank(message = "low is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "low must be a valid number >= 0")
        String low,

        @NotBlank(message = "close is required")
        @Pattern(regexp = "^(?:\\d+)(?:\\.\\d+)?$", message = "close must be a valid number >= 0")
        String close,

        @NotBlank(message = "volume is required")
        @Pattern(regexp = "^[1-9]\\d*$", message = "volume must be a valid integer > 0")
        String volume
) {}
