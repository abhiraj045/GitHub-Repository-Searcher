package com.example.githubsearcher.mapper;

import com.example.githubsearcher.dto.GithubSearchResponse;
import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.entity.GithubRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class GithubRepositoryMapper {

    /**
     * Copies GitHub data onto an entity (new or existing).(avoid duplicating)
     */
    public void updateEntity(GithubSearchResponse.Item item, GithubRepository entity) {
        entity.setId(item.id());
        entity.setName(item.name());
        entity.setFullName(item.fullName());
        entity.setDescription(item.description());
        entity.setOwnerName(item.owner() != null ? item.owner().login() : null);
        entity.setLanguage(item.language());
        entity.setStars(item.stars() != null ? item.stars() : 0);
        entity.setForks(item.forks() != null ? item.forks() : 0);
        entity.setWatchers(item.watchers() != null ? item.watchers() : 0);
        entity.setOpenIssues(item.openIssues() != null ? item.openIssues() : 0);
        entity.setHtmlUrl(item.htmlUrl());
        entity.setCreatedAt(item.createdAt());
        entity.setLastUpdated(item.updatedAt());
        entity.setPushedAt(item.pushedAt());
        entity.setSavedAt(Instant.now());
    }

    public RepositoryDto toDto(GithubRepository r) {
        return new RepositoryDto(
                r.getId(), r.getName(), r.getFullName(), r.getDescription(), r.getOwnerName(),
                r.getLanguage(), r.getStars(), r.getForks(), r.getWatchers(), r.getOpenIssues(),
                r.getHtmlUrl(), r.getCreatedAt(), r.getLastUpdated(), r.getPushedAt());
    }
}
