import type { AdminPermission } from '@/lib/permissions';
import admin from '@/routes/admin';
import type { LucideIcon } from 'lucide-react';
import {
    BarChart3,
    Bike,
    CreditCard,
    Headset,
    LayoutGrid,
    Map,
    Package,
    ScrollText,
    Settings,
    Store,
    Users,
} from 'lucide-react';

export type AdminNavItem = {
    title: string;
    /** Absent for modules that arrive in a later phase (shown as "Soon"). */
    href?: string;
    icon: LucideIcon;
    permission?: AdminPermission;
    superAdminOnly?: boolean;
    /** Extra paths that count as this section (detail pages). */
    match?: string[];
};

/** The sidebar from 02_Super_Admin_Workflow.png (§20). Orders → Support come with Phases W2/6. */
export const adminNav: AdminNavItem[] = [
    { title: 'Dashboard', href: admin.dashboard.url(), icon: LayoutGrid },
    {
        title: 'Franchises',
        href: admin.franchises.index.url(),
        icon: Map,
        permission: 'franchises.view',
        match: ['/admin/operator-applications'],
    },
    { title: 'Users', href: admin.users.index.url(), icon: Users, permission: 'users.view' },
    { title: 'Stores', href: admin.stores.index.url(), icon: Store, permission: 'partners.view' },
    { title: 'Riders', href: admin.riders.index.url(), icon: Bike, permission: 'partners.view' },
    { title: 'Orders', icon: Package },
    { title: 'Payments', icon: CreditCard },
    { title: 'Reports', icon: BarChart3 },
    { title: 'Support', icon: Headset },
    {
        title: 'Audit log',
        href: admin.audit.index.url(),
        icon: ScrollText,
        permission: 'audit.view',
    },
    {
        title: 'Settings',
        href: admin.settings.security.url(),
        icon: Settings,
        match: ['/admin/settings'],
    },
];
