package org.oleg.ai.challenge.domain.command

/**
 * Result of GitHub URL validation
 */
sealed interface GitHubUrlValidation {
    /**
     * URL is valid and has been normalized
     * @property normalizedUrl The normalized URL (always starts with https://github.com/)
     */
    data class Valid(val normalizedUrl: String) : GitHubUrlValidation

    /**
     * URL is invalid
     * @property reason Human-readable error message
     */
    data class Invalid(val reason: String) : GitHubUrlValidation
}

/**
 * Validates GitHub repository URLs without making API calls.
 *
 * Accepted formats:
 * - https://github.com/user/repo
 * - http://github.com/user/repo
 * - github.com/user/repo
 * - github.com/user/repo.git
 * - https://github.com/user/repo/issues (any path after repo name)
 *
 * All valid URLs are normalized to: https://github.com/username/repo
 */
class GitHubUrlValidator {
    companion object {
        /**
         * Regex pattern to match GitHub repository URLs
         * Groups: (1) username, (2) repository name
         */
        private val GITHUB_REPO_REGEX = Regex(
            pattern = """^(?:https?://)?(?:www\.)?github\.com/([a-zA-Z0-9_-]+)/([a-zA-Z0-9_.-]+?)(?:\.git)?(?:/.*)?$""",
            option = RegexOption.IGNORE_CASE
        )
    }

    /**
     * Validates and normalizes a GitHub repository URL
     *
     * @param url The URL to validate
     * @return GitHubUrlValidation.Valid with normalized URL, or GitHubUrlValidation.Invalid with error message
     */
    fun validate(url: String): GitHubUrlValidation {
        val trimmed = url.trim()

        if (trimmed.isBlank()) {
            return GitHubUrlValidation.Invalid("GitHub URL cannot be empty")
        }

        val match = GITHUB_REPO_REGEX.matchEntire(trimmed) ?: return GitHubUrlValidation.Invalid(
            "Invalid GitHub repository URL. Expected format: github.com/user/repo or https://github.com/user/repo"
        )

        val (username, repoName) = match.destructured
        val normalizedUrl = "https://github.com/$username/$repoName"

        return GitHubUrlValidation.Valid(normalizedUrl)
    }
}
