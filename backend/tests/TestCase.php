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
}
