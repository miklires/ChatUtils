package dev.miklires.chatutils.client.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainMatcherTest {
    @Test void emptyListAllowsValidHosts() {
        assertTrue(DomainMatcher.isAllowed("example.org", List.of()));
    }

    @Test void ruleAllowsExactHostAndSubdomains() {
        assertTrue(DomainMatcher.isAllowed("example.org", List.of("example.org")));
        assertTrue(DomainMatcher.isAllowed("cdn.example.org", List.of("https://example.org/path")));
    }

    @Test void ruleRejectsSuffixAndUserInfoTricks() {
        assertFalse(DomainMatcher.isAllowed("notexample.org", List.of("example.org")));
        assertFalse(DomainMatcher.isAllowed("example.org.evil.test", List.of("example.org")));
        assertFalse(DomainMatcher.isAllowed("", List.of("example.org")));
    }
}
