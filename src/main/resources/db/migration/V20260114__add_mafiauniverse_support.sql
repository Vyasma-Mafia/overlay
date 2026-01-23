-- Flyway Migration: Add MafiaUniverse support
-- Date: 2026-01-14
-- This migration creates the player_mafiauniverse_nickname table to map MafiaUniverse nicknames to Player IDs

CREATE TABLE IF NOT EXISTS player_mafiauniverse_nickname
(
    id         uuid PRIMARY KEY,
    player_id  uuid      NOT NULL,
    nickname   text      NOT NULL,
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT fk_player_mafiauniverse_nickname__player
        FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE CASCADE,
    CONSTRAINT uq_player_mafiauniverse_nickname UNIQUE (nickname)
);

CREATE INDEX IF NOT EXISTS idx_player_mafiauniverse_nickname_player_id
    ON player_mafiauniverse_nickname (player_id);
