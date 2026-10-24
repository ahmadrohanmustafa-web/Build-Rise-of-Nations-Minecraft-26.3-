package com.riseofnations.core.persist;

import com.riseofnations.core.economy.Ledger;
import com.riseofnations.core.economy.WageTracker;
import com.riseofnations.core.story.PlayerStory;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Everything that must survive a restart. One instance per world. */
public final class CoreState {

    private final Ledger ledger;
    private final WageTracker wages;
    private final Map<String, PlayerStory> stories = new TreeMap<>();

    public CoreState() {
        this(new Ledger(), new WageTracker());
    }

    public CoreState(Ledger ledger, WageTracker wages) {
        this.ledger = ledger;
        this.wages = wages;
    }

    public Ledger ledger() { return ledger; }

    public WageTracker wages() { return wages; }

    /** Returns the player's story, creating it on first use. Returns null for an unsafe id. */
    public PlayerStory story(String playerId) {
        if (!Ledger.validId(playerId)) return null;
        return stories.computeIfAbsent(playerId, k -> new PlayerStory());
    }

    public Map<String, PlayerStory> storiesView() {
        return Collections.unmodifiableMap(stories);
    }
}
