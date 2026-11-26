import {
    SidebarGroup,
    SidebarMenu,
    SidebarMenuBadge,
    SidebarMenuButton,
    SidebarMenuItem,
} from '@/components/ui/sidebar';
import { useCurrentUrl } from '@/hooks/use-current-url';
import type { AdminNavItem } from '@/lib/navigation';
import { Link } from '@inertiajs/react';

export function NavMain({ items }: { items: AdminNavItem[] }) {
    const { currentUrl } = useCurrentUrl();
    const isActive = (item: AdminNavItem) =>
        item.href !== undefined &&
        (item.href === '/admin'
            ? currentUrl === '/admin'
            : [item.href, ...(item.match ?? [])].some((path) => currentUrl.startsWith(path)));

    return (
        <SidebarGroup className="px-2 py-2">
            <SidebarMenu className="gap-1">
                {items.map((item) =>
                    item.href ? (
                        <SidebarMenuItem key={item.title}>
                            <SidebarMenuButton
                                asChild
                                isActive={isActive(item)}
                                tooltip={{ children: item.title }}
                                className="data-[active=true]:bg-sidebar-primary data-[active=true]:text-sidebar-primary-foreground"
                            >
                                <Link href={item.href} prefetch>
                                    <item.icon />
                                    <span>{item.title}</span>
                                </Link>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    ) : (
                        <SidebarMenuItem key={item.title}>
                            <SidebarMenuButton
                                disabled
                                aria-disabled
                                tooltip={{ children: `${item.title} — coming soon` }}
                                className="opacity-60"
                            >
                                <item.icon />
                                <span>{item.title}</span>
                            </SidebarMenuButton>
                            <SidebarMenuBadge className="text-[10px] tracking-wide text-sidebar-foreground/60 uppercase">
                                Soon
                            </SidebarMenuBadge>
                        </SidebarMenuItem>
                    ),
                )}
            </SidebarMenu>
        </SidebarGroup>
    );
}
