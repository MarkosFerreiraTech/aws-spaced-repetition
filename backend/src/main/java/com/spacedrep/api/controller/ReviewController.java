package com.spacedrep.api.controller;

import com.spacedrep.api.dto.AnswerRequest;
import com.spacedrep.api.dto.AnswerResponse;
import com.spacedrep.api.dto.DueQueueResponse;
import com.spacedrep.api.mapper.ReviewMapper;
import com.spacedrep.application.CurrentUserProvider;
import com.spacedrep.application.ReviewService;
import com.spacedrep.application.command.AnswerCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de estudo. Controller magro: valida, delega, converte.
 */
@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserProvider currentUser;

    public ReviewController(ReviewService reviewService, CurrentUserProvider currentUser) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
    }

    @GetMapping("/due")
    public DueQueueResponse dueCards(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ReviewMapper.toResponse(reviewService.getDueQueue(currentUser.currentUserId(), limit));
    }

    @PostMapping("/{cardId}/answer")
    public AnswerResponse answer(@PathVariable long cardId, @Valid @RequestBody AnswerRequest request) {
        AnswerCommand command = new AnswerCommand(request.selectedOptionIds(), request.durationMs());
        return ReviewMapper.toResponse(
                reviewService.answer(currentUser.currentUserId(), cardId, command));
    }
}
