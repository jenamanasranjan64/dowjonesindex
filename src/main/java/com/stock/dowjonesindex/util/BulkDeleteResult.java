package com.stock.dowjonesindex.util;

import java.util.List;

public record BulkDeleteResult(List<Long> deletedIds, int deletedCount) {}

