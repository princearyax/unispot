package com.prince.unispot.place.presentation.dto;

import java.util.List;

public record CursorPageResponse<T>(
    List<T> items, //can decide dynamically
    Long nextCursor,
    boolean hasNext
) {}