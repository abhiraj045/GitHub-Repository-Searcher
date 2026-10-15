package com.example.githubsearcher.service;

import com.example.githubsearcher.client.GithubSearchClient;
import com.example.githubsearcher.dto.GithubSearchResponse;
import com.example.githubsearcher.dto.RepositoryDto;
import com.example.githubsearcher.dto.SearchRequest;
import com.example.githubsearcher.dto.SortField;
import com.example.githubsearcher.entity.GithubRepository;
import com.example.githubsearcher.mapper.GithubRepositoryMapper;
import com.example.githubsearcher.repository.GithubRepositoryRepository;
import com.example.githubsearcher.repository.GithubRepositorySpecifications;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GithubRepositoryService {

    private final GithubSearchClient githubClient;
    private final GithubRepositoryRepository repository;
    private final GithubRepositoryMapper mapper;
    private final int defaultPerPage;

    public GithubRepositoryService(GithubSearchClient githubClient,
            GithubRepositoryRepository repository,
            GithubRepositoryMapper mapper,
            @Value("${github.api.default-per-page:30}") int defaultPerPage) {
        this.githubClient = githubClient;
        this.repository = repository;
        this.mapper = mapper;
        this.defaultPerPage = defaultPerPage;
    }

    /**
     * Fetches from GitHub, then inserts new repositories and updates existing
     * ones (matched by GitHub id).
     */
    @Transactional
    public List<RepositoryDto> searchAndSave(SearchRequest request) {
        SortField sort = SortField.from(request.sort());
        int perPage = request.perPage() != null ? request.perPage() : defaultPerPage;

        GithubSearchResponse response
                = githubClient.search(request.query(), request.language(), sort, perPage);

        if (response.items() == null || response.items().isEmpty()) {
            return List.of();
        }

        List<GithubSearchResponse.Item> items = response.items().stream()
                .filter(i -> i != null && i.id() != null && i.name() != null)
                .toList();

        Map<Long, GithubRepository> existing = repository
                .findAllById(items.stream().map(GithubSearchResponse.Item::id).toList())
                .stream()
                .collect(Collectors.toMap(GithubRepository::getId, Function.identity()));

        List<GithubRepository> toSave = new ArrayList<>();
        for (GithubSearchResponse.Item item : items) {
            GithubRepository entity = existing.getOrDefault(item.id(), new GithubRepository());
            mapper.updateEntity(item, entity);
            toSave.add(entity);
        }

        return repository.saveAll(toSave).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RepositoryDto> findStored(String language, Integer minStars, String sortBy) {
        SortField sort = SortField.from(sortBy);

        Specification<GithubRepository> spec = (root, query, cb) -> cb.conjunction();
        if (language != null && !language.isBlank()) {
            spec = spec.and(GithubRepositorySpecifications.hasLanguage(language));
        }
        if (minStars != null) {
            spec = spec.and(GithubRepositorySpecifications.hasMinStars(minStars));
        }

        Sort order = Sort.by(
                new Sort.Order(Sort.Direction.DESC, sort.getEntityField()),
                new Sort.Order(Sort.Direction.ASC, "id"));

        return repository.findAll(spec, order).stream().map(mapper::toDto).toList();
    }
}
