package com.riseofnations.core.civ;

/**
 * Exactly what a player's client is allowed to see about their own civilization.
 * It is a read-only snapshot built on the server; the client can never send one back.
 */
public record CivView(String name, String leaderName, String colorId, String governmentId, long treasury,
                      long population, int settlements, String stageId, String economyId, long createdAtEpochMs) { }
