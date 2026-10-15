package com.example.githubsearcher.client;

import com.example.githubsearcher.dto.GithubSearchResponse;
import com.example.githubsearcher.dto.SortField;

public interface GithubSearchClient {

    GithubSearchResponse search(String query, String language, SortField sort, int perPage);
}
