<?php

namespace App\Support;

/**
 * Patterns for `like` / `ilike` searches with the user's text taken literally (% and _ escaped).
 */
final class Like
{
    public static function contains(string $term): string
    {
        return '%'.self::escape(trim($term)).'%';
    }

    public static function startsWith(string $term): string
    {
        return self::escape(trim($term)).'%';
    }

    private static function escape(string $term): string
    {
        return addcslashes($term, '%_\\');
    }
}
