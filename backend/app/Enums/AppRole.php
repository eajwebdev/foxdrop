<?php

namespace App\Enums;

/**
 * Mirrors the `app_role` Postgres enum (MASTER_PROMPT §5). The value is the exact database label.
 */
enum AppRole: string
{
    case Customer = 'customer';
    case Rider = 'rider';
    case StoreOwner = 'store_owner';
    case StoreStaff = 'store_staff';
    case Operator = 'operator';
    case OperatorStaff = 'operator_staff';
    case SuperAdmin = 'super_admin';
    case AdminStaff = 'admin_staff';

    /**
     * @return list<self>
     */
    public static function admin(): array
    {
        return [self::SuperAdmin, self::AdminStaff];
    }

    /**
     * @return list<self>
     */
    public static function operatorTeam(): array
    {
        return [self::Operator, self::OperatorStaff];
    }

    /**
     * @return list<self>
     */
    public static function storeTeam(): array
    {
        return [self::StoreOwner, self::StoreStaff];
    }
}
