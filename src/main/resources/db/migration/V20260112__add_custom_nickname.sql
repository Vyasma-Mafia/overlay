-- Flyway Migration: Add custom_nickname column to player table
-- Date: 2026-01-12
-- This migration adds support for manual nickname override while preserving external service nickname

ALTER TABLE player
    ADD COLUMN IF NOT EXISTS custom_nickname text;

