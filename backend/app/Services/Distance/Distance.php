<?php

namespace App\Services\Distance;

/**
 * A distance in km and how it was measured (`routes` = Google driving distance, `straight_line` = fallback).
 */
final readonly class Distance
{
    public function __construct(public float $km, public string $source) {}
}
