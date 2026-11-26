import type { AdminPermission } from '@/lib/permissions';

/** The signed-in admin, as shared by HandleInertiaRequests. */
export type User = {
    id: string;
    full_name: string | null;
    email: string | null;
    is_super_admin: boolean;
    permissions: AdminPermission[];
    two_factor_enabled: boolean;
};

export type Auth = {
    user: User | null;
    /** Unix seconds until "confirm it's you" stops counting; null when it must be asked again. */
    confirmedUntil: number | null;
};
