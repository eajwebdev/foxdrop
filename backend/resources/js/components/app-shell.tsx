import { SidebarProvider } from '@/components/ui/sidebar';
import { usePage } from '@inertiajs/react';
import type { ReactNode } from 'react';

export function AppShell({ children }: { children: ReactNode }) {
    return <SidebarProvider defaultOpen={usePage().props.sidebarOpen}>{children}</SidebarProvider>;
}
