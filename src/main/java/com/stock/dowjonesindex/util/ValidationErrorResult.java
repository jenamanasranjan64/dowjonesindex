package com.stock.dowjonesindex.util;

import java.util.List;
import java.util.Map;

public record ValidationErrorResult(
        List<String> errorFields,
        Map<String, List<String>> fieldErrors,
        String detail
) {}
