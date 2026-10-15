package com.example.githubsearcher.repository;

import com.example.githubsearcher.entity.GithubRepository;
import org.springframework.data.jpa.domain.Specification;

public final class GithubRepositorySpecifications {

    private GithubRepositorySpecifications() {
    }

    public static Specification<GithubRepository> hasLanguage(String language) {
        String lang = language.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get("language")), lang);
    }

    public static Specification<GithubRepository> hasMinStars(int minStars) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Integer>get("stars"), minStars);
    }
}
