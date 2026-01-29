package com.stock.dowjonesindex.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkDeleteResult(
        @JsonProperty("deletedStockIds") List<Long> deletedIds,
        @JsonProperty("deletedStockIdCount") Integer deletedIdCount,
        @JsonProperty("notFoundStockIds") List<Long> notFoundIds,
        @JsonProperty("notFoundStockIdCount") Integer notFoundIdCount
) {}
