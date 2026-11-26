import { AppearanceToggle } from '@/components/appearance-toggle';
import { Breadcrumbs } from '@/components/breadcrumbs';
import { CommandPalette } from '@/components/command-palette';
import { SidebarTrigger } from '@/components/ui/sidebar';
import type { BreadcrumbItem as BreadcrumbItemType } from '@/types';

export function AppSidebarHeader({ breadcrumbs = [] }: { breadcrumbs?: BreadcrumbItemType[] }) {
    return (
        <header className="sticky top-0 z-20 flex h-16 shrink-0 items-center gap-3 border-b bg-background/90 px-4 backdrop-blur md:px-6">
            <SidebarTrigger className="-ml-1" />
            <div className="hidden min-w-0 flex-1 md:block">
                <Breadcrumbs breadcrumbs={breadcrumbs} />
            </div>
            <div className="ml-auto flex items-center gap-2">
                <CommandPalette />
                <AppearanceToggle />
            </div>
        </header>
    );
}
