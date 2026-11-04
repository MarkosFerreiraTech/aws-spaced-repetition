package com.spacedrep.api.dto;

import java.util.List;

public record DueQueueResponse(List<CardResponse> cards, long remaining) {
}
