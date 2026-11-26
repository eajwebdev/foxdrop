import { useCurrentUrl } from '@/hooks/use-current-url';
import { cn } from '@/lib/utils';
import admin from '@/routes/admin';
import { Link, usePage } from '@inertiajs/react';

export function SettingsTabs() {
    const user = usePage().props.auth.user;
    const { currentUrl } = useCurrentUrl();
    const tabs = [
        { title: 'My security', href: admin.settings.security.url() },
        ...(user?.is_super_admin ? [{ title: 'Admin staff', href: admin.staff.index.url() }] : []),
    ];

    return (
        <nav className="flex gap-1 border-b" aria-label="Settings">
            {tabs.map((tab) => (
                <Link
                    key={tab.href}
                    href={tab.href}
                    className={cn(
                        '-mb-px border-b-2 px-3 py-2 text-sm font-medium transition-colors',
                        currentUrl === tab.href
                            ? 'border-brand text-foreground'
                            : 'border-transparent text-muted-foreground hover:text-foreground',
                    )}
                >
                    {tab.title}
                </Link>
            ))}
        </nav>
    );
}
