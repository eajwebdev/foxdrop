<?php

namespace App\Enums;

/**
 * What an `admin_staff` account may do in the admin web (DECISIONS 72). `super_admin` holds every permission.
 * The value is stored in `admin_permissions.permission` (its check lists the same values).
 */
enum AdminPermission: string
{
    case FranchisesView = 'franchises.view';
    case FranchisesManage = 'franchises.manage';
    case PartnersView = 'partners.view';
    case PartnersManage = 'partners.manage';
    case UsersView = 'users.view';
    case UsersManage = 'users.manage';
    case AuditView = 'audit.view';

    public function label(): string
    {
        return match ($this) {
            self::FranchisesView => 'See provinces, licenses and operator applications',
            self::FranchisesManage => 'Approve operators and change licenses',
            self::PartnersView => 'See stores and riders',
            self::PartnersManage => 'Approve, suspend and reinstate stores and riders',
            self::UsersView => 'See user accounts',
            self::UsersManage => 'Suspend and reactivate user accounts',
            self::AuditView => 'Read the audit log',
        };
    }

    /**
     * @return list<string>
     */
    public static function names(): array
    {
        return array_map(fn (self $permission): string => $permission->value, self::cases());
    }
}
