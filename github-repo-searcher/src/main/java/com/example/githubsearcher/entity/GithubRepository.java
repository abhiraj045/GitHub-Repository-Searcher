package com.example.githubsearcher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A GitHub repository stored locally. The primary key is the GitHub repository id,
 * so saving the same repository twice updates the row instead of duplicating it.
 */
@Entity
@Table(name = "github_repositories", indexes = {
        @Index(name = "idx_repo_language", columnList = "language"),
        @Index(name = "idx_repo_stars", columnList = "stars")
})
@Getter
@Setter
@NoArgsConstructor
public class GithubRepository {

    /** GitHub's own repository id (not generated locally). */
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ownerName;

    private String language;

    private int stars;

    private int forks;

    private int watchers;

    private int openIssues;

    @Column(length = 1000)
    private String htmlUrl;

    private Instant createdAt;

    /** GitHub's "updated_at". */
    private Instant lastUpdated;

    private Instant pushedAt;

    /** When this row was last refreshed from GitHub. */
    private Instant savedAt;
}
