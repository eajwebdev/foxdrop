import { usePage } from '@inertiajs/react';

/** The current page's path (no query string), for highlighting the matching navigation item. */
export function useCurrentUrl(): { currentUrl: string } {
    const url = usePage().url;

    return {
        currentUrl: new URL(
            url,
            typeof window !== 'undefined' ? window.location.origin : 'http://localhost',
        ).pathname,
    };
}
