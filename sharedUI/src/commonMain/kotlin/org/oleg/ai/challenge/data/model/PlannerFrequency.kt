package org.oleg.ai.challenge.data.model

/**
 * Predefined frequency options for planner periodic execution.
 */
enum class PlannerFrequency(val milliseconds: Long, val displayName: String) {
    FIVE_SECONDS(5_000L, "5 seconds"),
    TEN_SECONDS(10_000L, "10 seconds"),
    THIRTY_SECONDS(30_000L, "30 seconds"),
    ONE_MINUTE(60_000L, "1 minute"),
    FIVE_MINUTES(300_000L, "5 minutes"),
    TEN_MINUTES(600_000L, "10 minutes");

    companion object {
        val DEFAULT = THIRTY_SECONDS
    }
}
