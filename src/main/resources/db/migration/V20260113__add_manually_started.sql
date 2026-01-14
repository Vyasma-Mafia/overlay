-- Flyway Migration: Add manually_started column to game table
-- Date: 2026-01-13
-- This migration adds support for preventing automatic systems from resetting started=true
-- when a user has manually set started=false via UI

ALTER TABLE game
    ADD COLUMN IF NOT EXISTS manually_started boolean DEFAULT true;
