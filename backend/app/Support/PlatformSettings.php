<?php

namespace App\Support;

use Illuminate\Support\Facades\DB;

/**
 * The single `platform_settings` row (§14 defaults, changed by the super admin in Phase W2).
 */
final class PlatformSettings
{
    public static function licenseFeeCentavos(): int
    {
        return (int) DB::table('platform_settings')->value('license_fee_centavos');
    }

    public static function licenseTermDays(): int
    {
        return (int) DB::table('platform_settings')->value('license_term_days');
    }

    public static function licenseGraceDays(): int
    {
        return (int) DB::table('platform_settings')->value('license_grace_days');
    }
}
