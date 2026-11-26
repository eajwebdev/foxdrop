import { router } from '@inertiajs/react';
import type { ClassValue } from 'clsx';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

/**
 * Visits the current page again with [changes] applied to its query string (filters, page number). Filters live in
 * the URL (§20.5 saved filters), so a list can be shared and the back button works.
 */
export function visitWithQuery(
    changes: Record<string, string | null>,
    options: { resetPage?: boolean } = {},
) {
    const url = new URL(window.location.href);
    if (options.resetPage) {
        url.searchParams.delete('page');
    }
    for (const [key, value] of Object.entries(changes)) {
        url.searchParams.set(key, value ?? '');
    }
    router.get(
        url.pathname + url.search,
        {},
        { preserveState: true, replace: options.resetPage === true },
    );
}
