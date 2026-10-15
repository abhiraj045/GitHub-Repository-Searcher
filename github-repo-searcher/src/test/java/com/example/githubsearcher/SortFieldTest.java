package com.example.githubsearcher;

import com.example.githubsearcher.dto.SortField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortFieldTest {

    @Test
    void defaultsToStars() {
        assertEquals(SortField.STARS, SortField.from(null));
        assertEquals(SortField.STARS, SortField.from(" "));
    }

    @Test
    void parsesCaseInsensitively() {
        assertEquals(SortField.FORKS, SortField.from("FORKS"));
        assertEquals("lastUpdated", SortField.from("updated").getEntityField());
    }

    @Test
    void rejectsUnknownValues() {
        assertThrows(IllegalArgumentException.class, () -> SortField.from("popularity"));
    }
}
