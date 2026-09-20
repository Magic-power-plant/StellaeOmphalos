package com.mpp.stellaeomphalos.constellation.boon;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Free skill point tokens. Tokens are a set of unique names, so granting the same fixed token
 * twice is a no-op (idempotent) and revoking removes exactly the tokens recorded for a source.
 */
public final class BoonFreePointLedger {

    private final Set<String> tokens = new LinkedHashSet<>();

    /** @return true when the token was newly granted; false when it was already present. */
    public boolean grant(String token) {
        if (token == null || token.isEmpty()) throw new IllegalArgumentException("empty token");
        return tokens.add(token);
    }

    public void grantAll(Collection<String> granted) {
        granted.forEach(this::grant);
    }

    /** @return true when the token existed and was reclaimed. */
    public boolean revoke(String token) {
        return tokens.remove(token);
    }

    /** Reclaims exactly the given tokens (used when the granting source is removed). */
    public void revokeAll(Collection<String> reclaimed) {
        reclaimed.forEach(tokens::remove);
    }

    public int count() {
        return tokens.size();
    }

    public boolean contains(String token) {
        return tokens.contains(token);
    }

    public List<String> tokens() {
        return List.copyOf(tokens);
    }

    public void clear() {
        tokens.clear();
    }
}
