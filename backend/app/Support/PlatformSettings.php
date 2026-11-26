<?php

namespace App\Support;

use Illuminate\Support\Facades\DB;

/**
 * The single `platform_settings` row (§14 defaults, changed by the super admin in Phase W2). Each value is read once
 * per request.
 */
final class PlatformSettings
{
    public static function licenseFeeCentavos(): int
    {
        return (int) once(fn () => DB::table('platform_settings')->value('license_fee_centavos'));
    }

    public static function licenseTermDays(): int
    {
        return (int) once(fn () => DB::table('platform_settings')->value('license_term_days'));
    }

    public static function licenseGraceDays(): int
    {
        return (int) once(fn () => DB::table('platform_settings')->value('license_grace_days'));
    }

    public static function platformCommissionBps(): int
    {
        return (int) once(fn () => DB::table('platform_settings')->value('platform_commission_bps'));
    }

    /** The most an operator may take from item sales (§6); a province's own rate above it is capped. */
    public static function operatorCommissionCapBps(): int
    {
        return (int) once(fn () => DB::table('platform_settings')->value('operator_commission_cap_bps'));
    }

    /** An online order not paid within this many seconds is cancelled (§14: 15 min). */
    public static function unpaidOrderTimeoutSeconds(): int
    {
        return (int) once(fn () => DB::table('platform_settings')->value('unpaid_order_timeout_seconds'));
    }

    /** A placed order the store does not answer within this many seconds is cancelled (§14: 5 min). */
    public static function storeResponseTimeoutSeconds(): int
    {
        return (int) once(fn () => DB::table('platform_settings')->value('store_response_timeout_seconds'));
    }
}
