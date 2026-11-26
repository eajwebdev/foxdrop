<?php

namespace Tests;

use Illuminate\Foundation\Testing\TestCase as BaseTestCase;

abstract class TestCase extends BaseTestCase
{
    /**
     * The schema defines Postgres enums and a view; a fresh test database must drop them too.
     */
    protected bool $dropTypes = true;

    protected bool $dropViews = true;

    /**
     * Provinces + demo data are seeded once per test run (with the schema); each test rolls back its own changes.
     */
    protected bool $seed = true;
}
