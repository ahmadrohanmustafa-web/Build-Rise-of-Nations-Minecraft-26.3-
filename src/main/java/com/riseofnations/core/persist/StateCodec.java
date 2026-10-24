package com.riseofnations.core.persist;

import com.riseofnations.core.economy.Ledger;
import com.riseofnations.core.economy.WageTracker;
import com.riseofnations.core.story.PlayerStory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns CoreState into a versioned text block and back. Pure Java.
 *
 * Safety rules:
 *  - Bad or unknown lines are skipped with a warning; they never crash loading.
 *  - A save from a NEWER schema is refused (refused=true). The caller must keep the raw text and
 *    must not overwrite it, so a downgrade cannot destroy data.
 *  - Negative balances and unsafe ids are dropped; the ledger is reconciled.
 */
public final class StateCodec {

    public static final int SCHEMA_VERSION = 1;
    private static final String HEADER = "ron-state";

    public record Decoded(CoreState state, List<String> warnings, boolean refused) { }

    private StateCodec() { }

    public static String encode(CoreState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\t').append(SCHEMA_VERSION).append('\n');
        Ledger l = state.ledger();
        sb.append("minted\t").append(l.totalMinted()).append('\n');
        sb.append("burned\t").append(l.totalBurned()).append('\n');
        for (Map.Entry<String, Long> e : l.balancesView().entrySet()) {
            sb.append("bal\t").append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        }
        for (Map.Entry<String, Long> e : state.wages().lastPaidView().entrySet()) {
            int bar = e.getKey().indexOf('|');
            sb.append("wage\t").append(e.getKey(), 0, bar).append('\t').append(e.getKey().substring(bar + 1))
                    .append('\t').append(e.getValue()).append('\n');
        }
        for (Map.Entry<String, long[]> e : state.wages().dailyView().entrySet()) {
            sb.append("daily\t").append(e.getKey()).append('\t').append(e.getValue()[0]).append('\t')
                    .append(e.getValue()[1]).append('\n');
        }
        for (Map.Entry<String, PlayerStory> pe : state.storiesView().entrySet()) {
            String p = pe.getKey();
            PlayerStory s = pe.getValue();
            if (s.started()) sb.append("started\t").append(p).append('\n');
            sb.append("rep\t").append(p).append('\t').append(s.reputation()).append('\n');
            for (Map.Entry<String, PlayerStory.Status> e : s.statusView().entrySet()) {
                sb.append("st\t").append(p).append('\t').append(e.getKey()).append('\t').append(e.getValue()).append('\n');
            }
            for (Map.Entry<String, Long> e : s.progressView().entrySet()) {
                int slash = e.getKey().indexOf('/');
                sb.append("pg\t").append(p).append('\t').append(e.getKey(), 0, slash).append('\t')
                        .append(e.getKey().substring(slash + 1)).append('\t').append(e.getValue()).append('\n');
            }
            for (String f : s.flagsView()) sb.append("flag\t").append(p).append('\t').append(f).append('\n');
            for (String f : s.rewardsPaidView()) sb.append("paid\t").append(p).append('\t').append(f).append('\n');
            for (String f : s.unlocksView()) sb.append("unlock\t").append(p).append('\t').append(f).append('\n');
            for (String k : s.seenKeysView()) {
                int bar = k.indexOf('|');
                sb.append("seen\t").append(p).append('\t').append(k, 0, bar).append('\t').append(k.substring(bar + 1)).append('\n');
            }
        }
        return sb.toString();
    }

    public static Decoded decode(String text) {
        List<String> warnings = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return new Decoded(new CoreState(), warnings, false);   // brand new world
        }
        String[] lines = text.split("\n", -1);
        String[] head = lines[0].split("\t", -1);
        if (head.length != 2 || !HEADER.equals(head[0])) {
            warnings.add("Save data has no valid header; refusing to load it.");
            return new Decoded(new CoreState(), warnings, true);
        }
        int version;
        try {
            version = Integer.parseInt(head[1].trim());
        } catch (NumberFormatException e) {
            warnings.add("Save data version is not a number; refusing to load it.");
            return new Decoded(new CoreState(), warnings, true);
        }
        if (version > SCHEMA_VERSION || version < 1) {
            warnings.add("Save data is schema " + version + " but this mod understands up to " + SCHEMA_VERSION
                    + "; refusing to load so nothing is overwritten.");
            return new Decoded(new CoreState(), warnings, true);
        }

        long minted = 0;
        long burned = 0;
        Map<String, Long> balances = new TreeMap<>();
        WageTracker wages = new WageTracker();
        CoreState[] holder = new CoreState[1];
        Map<String, PlayerStory> stories = new TreeMap<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;
            String[] f = line.split("\t", -1);
            try {
                switch (f[0]) {
                    case "minted" -> minted = Long.parseLong(need(f, 2)[1]);
                    case "burned" -> burned = Long.parseLong(need(f, 2)[1]);
                    case "bal" -> balances.put(need(f, 3)[1], Long.parseLong(f[2]));
                    case "wage" -> {
                        need(f, 4);
                        if (!Ledger.validId(f[1]) || !Ledger.validId(f[2])) throw new IllegalArgumentException("unsafe id");
                        wages.restoreLastPaid(f[1] + "|" + f[2], Long.parseLong(f[3]));
                    }
                    case "daily" -> {
                        need(f, 4);
                        wages.restoreDaily(f[1], Long.parseLong(f[2]), Long.parseLong(f[3]));
                    }
                    case "started", "rep", "st", "pg", "flag", "paid", "unlock", "seen" -> {
                        if (f.length < 2 || !Ledger.validId(f[1])) throw new IllegalArgumentException("unsafe player id");
                        PlayerStory s = stories.computeIfAbsent(f[1], k -> new PlayerStory());
                        applyStoryLine(s, f);
                    }
                    default -> warnings.add("line " + (i + 1) + ": unknown record '" + f[0] + "' skipped");
                }
            } catch (RuntimeException e) {
                warnings.add("line " + (i + 1) + ": skipped (" + e.getMessage() + ")");
            }
        }

        Ledger.Restored restored = Ledger.restore(balances, minted, burned);
        if (restored.reconciled()) {
            warnings.add("Ledger totals were inconsistent and have been reconciled ("
                    + restored.droppedAccounts() + " bad accounts dropped).");
        }
        holder[0] = new CoreState(restored.ledger(), wages);
        for (Map.Entry<String, PlayerStory> e : stories.entrySet()) {
            PlayerStory target = holder[0].story(e.getKey());
            copyInto(target, e.getValue());
        }
        return new Decoded(holder[0], warnings, false);
    }

    private static void applyStoryLine(PlayerStory s, String[] f) {
        switch (f[0]) {
            case "started" -> {
                need(f, 2);
                s.restoreStarted(true);
            }
            case "rep" -> s.restoreReputation(Integer.parseInt(need(f, 3)[2]));
            case "st" -> {
                need(f, 4);
                s.restoreStatus(f[2], PlayerStory.Status.valueOf(f[3]));
            }
            case "pg" -> {
                need(f, 5);
                s.restoreProgress(f[2] + "/" + f[3], Long.parseLong(f[4]));
            }
            case "flag" -> s.restoreFlag(safe(need(f, 3)[2]));
            case "paid" -> s.restorePaid(safe(need(f, 3)[2]));
            case "unlock" -> s.restoreUnlock(safe(need(f, 3)[2]));
            case "seen" -> {
                need(f, 4);
                s.restoreSeen(safe(f[2]) + "|" + safe(f[3]));
            }
            default -> throw new IllegalArgumentException("unreachable");
        }
    }

    /** Applies decoded values onto the CoreState-owned PlayerStory instance. */
    private static void copyInto(PlayerStory target, PlayerStory from) {
        target.restoreStarted(from.started());
        target.restoreReputation(from.reputation());
        from.statusView().forEach(target::restoreStatus);
        from.progressView().forEach(target::restoreProgress);
        from.flagsView().forEach(target::restoreFlag);
        from.rewardsPaidView().forEach(target::restorePaid);
        from.unlocksView().forEach(target::restoreUnlock);
        from.seenKeysView().forEach(target::restoreSeen);
    }

    private static String[] need(String[] f, int count) {
        if (f.length != count) throw new IllegalArgumentException("expected " + count + " fields, got " + f.length);
        return f;
    }

    private static String safe(String id) {
        if (!Ledger.validId(id)) throw new IllegalArgumentException("unsafe id");
        return id;
    }
}
