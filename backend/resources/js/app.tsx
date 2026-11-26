import { Toaster } from '@/components/ui/sonner';
import { TooltipProvider } from '@/components/ui/tooltip';
import { initializeTheme } from '@/hooks/use-appearance';
import AppLayout from '@/layouts/app/app-sidebar-layout';
import AuthLayout from '@/layouts/auth/auth-simple-layout';
import { createInertiaApp } from '@inertiajs/react';

void createInertiaApp({
    title: (title) => (title ? `${title} · FoxDrop Admin` : 'FoxDrop Admin'),
    layout: (name) => (name.startsWith('auth/') || name === 'error' ? AuthLayout : AppLayout),
    strictMode: true,
    withApp(app) {
        return (
            <TooltipProvider delayDuration={0}>
                {app}
                <Toaster richColors closeButton />
            </TooltipProvider>
        );
    },
    progress: {
        color: '#F2014F',
    },
});

initializeTheme();
