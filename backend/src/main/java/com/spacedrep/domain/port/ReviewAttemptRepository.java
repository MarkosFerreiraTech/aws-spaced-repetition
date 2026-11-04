package com.spacedrep.domain.port;

import com.spacedrep.domain.model.ReviewAttempt;

public interface ReviewAttemptRepository {

    ReviewAttempt save(ReviewAttempt attempt);
}
