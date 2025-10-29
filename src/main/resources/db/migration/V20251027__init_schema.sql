-- Flyway Migration: Initialize base schema for overlay
-- Date: 2025-10-28
-- This migration creates tables based on JPA entities using snake_case column names.

-- Table: player
CREATE TABLE IF NOT EXISTS player
(
    id          uuid PRIMARY KEY,
    nickname    text NOT NULL,
    polemica_id bigint,
    gomafia_id  bigint
);

-- Table: player_photo
CREATE TABLE IF NOT EXISTS player_photo
(
    id              uuid PRIMARY KEY,
    player_id       uuid        NOT NULL,
    url             text        NOT NULL,
    type            varchar(50) NOT NULL,
    tournament_type varchar(50),
    tournament_id   bigint,
    deleted         boolean     NOT NULL DEFAULT false,
    CONSTRAINT fk_player_photo__player FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE CASCADE
);

-- Table: game
CREATE TABLE IF NOT EXISTS game
(
    id                  uuid PRIMARY KEY,
    type                varchar(50) NOT NULL,
    tournament_id       integer,
    game_num            integer,
    table_num           integer,
    phase               integer,
    started             boolean,
    visible_overlay     boolean              DEFAULT true,
    visible_roles       boolean              DEFAULT true,
    visible_scores      boolean              DEFAULT true,
    text                text,
    result              text,
    delay               integer     NOT NULL DEFAULT 0,
    auto_next_game      boolean              DEFAULT true,
    vote_candidates     jsonb       NOT NULL DEFAULT '[]'::jsonb,
    crawl_failure_count integer,
    last_crawl_error    text,
    last_failure_time   timestamp,
    crawl_stop_reason   text,
    version             bigint
);

-- Unique index equivalent to entity-level UniqueConstraint
CREATE UNIQUE INDEX IF NOT EXISTS ux_game_tournament_game_table_phase_type
    ON game (tournament_id, game_num, table_num, phase, type);

-- Table: game_player
CREATE TABLE IF NOT EXISTS game_player
(
    id               uuid PRIMARY KEY,
    game_id          uuid    NOT NULL,
    nickname         text    NOT NULL,
    place            integer NOT NULL DEFAULT 1,
    photo_url        text,
    role             text,
    status           text,
    fouls            integer          DEFAULT 0,
    techs            integer          DEFAULT 0,
    speaker          boolean          DEFAULT false,
    voting           boolean          DEFAULT false,
    club_icon        text,
    score            double precision,
    checks           jsonb   NOT NULL DEFAULT '[]'::jsonb,
    guess            jsonb   NOT NULL DEFAULT '[]'::jsonb,
    voted_by         jsonb   NOT NULL DEFAULT '[]'::jsonb,
    stat             jsonb   NOT NULL DEFAULT '{}'::jsonb,
    custom_photo     boolean,
    source_player_id bigint,
    CONSTRAINT fk_game_player__game FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);

-- Table: facts
CREATE TABLE IF NOT EXISTS facts
(
    id                       uuid PRIMARY KEY,
    game_id                  uuid          NOT NULL,
    text                     varchar(1000) NOT NULL,
    player_nickname          varchar(255),
    player_photo_url         varchar(500),
    stage_type               varchar(50)   NOT NULL,
    stage_day                integer,
    stage_player             integer,
    stage_voting             integer,
    display_duration_seconds integer       NOT NULL,
    is_displayed             boolean       NOT NULL DEFAULT false,
    CONSTRAINT fk_facts__game FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);

-- Table: tournament_overlay_settings
CREATE TABLE IF NOT EXISTS tournament_overlay_settings
(
    id              uuid PRIMARY KEY,
    game_type       varchar(50) NOT NULL,
    tournament_id   bigint      NOT NULL,
    overlay_enabled boolean     NOT NULL DEFAULT true,
    created_at      timestamp,
    updated_at      timestamp
);

-- Unique constraint via unique index for (game_type, tournament_id)
CREATE UNIQUE INDEX IF NOT EXISTS ux_tournament_overlay_settings_type_tournament
    ON tournament_overlay_settings (game_type, tournament_id);

-- Table: tournament_usage_log
CREATE TABLE IF NOT EXISTS tournament_usage_log
(
    tournament_id bigint PRIMARY KEY,
    game_count    bigint    NOT NULL DEFAULT 0,
    tables        jsonb     NOT NULL DEFAULT '[]'::jsonb,
    created_at    timestamp NOT NULL DEFAULT now(),
    updated_at    timestamp NOT NULL DEFAULT now()
);

-- Table: game_usage_log
CREATE TABLE IF NOT EXISTS game_usage_log
(
    id            bigserial PRIMARY KEY,
    game_id       uuid      NOT NULL,
    tournament_id bigint    NOT NULL,
    table_num     integer   NOT NULL,
    created_at    timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_game_usage_log_game_tournament UNIQUE (game_id, tournament_id),
    CONSTRAINT fk_game_usage_log__game FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);
