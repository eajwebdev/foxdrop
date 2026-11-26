import { usePage } from '@inertiajs/react';

/** Mirrors App\Enums\AdminPermission (DECISIONS 72). The server checks every one of them again. */
export type AdminPermission =
    | 'franchises.view'
    | 'franchises.manage'
    | 'partners.view'
    | 'partners.manage'
    | 'users.view'
    | 'users.manage'
    | 'audit.view';

/** Whether the signed-in admin may do [permission]; used to hide buttons, never to protect anything. */
export function useCan(): (permission: AdminPermission) => boolean {
    const user = usePage().props.auth.user;

    return (permission) => user?.permissions.includes(permission) ?? false;
}
