package com.spacedrep.api.dto;

import java.util.List;

public record DraftQuestionDto(String stem, String topic, boolean multiSelect,
                               String explanation, List<DraftOptionDto> options) {
}
