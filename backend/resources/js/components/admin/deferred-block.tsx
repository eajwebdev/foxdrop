import { ErrorState } from '@/components/admin/page';
import { Deferred, router } from '@inertiajs/react';
import type { ReactNode } from 'react';

/**
 * A deferred prop with its three states (CLAUDE.md "every screen"): skeleton while loading, error + retry if the
 * server failed (props are deferred with `rescue`), then the content.
 */
export function DeferredBlock({
    data,
    fallback,
    children,
}: {
    data: string | string[];
    fallback: ReactNode;
    children: ReactNode;
}) {
    const only = Array.isArray(data) ? data : [data];

    return (
        <Deferred
            data={data}
            fallback={fallback}
            rescue={<ErrorState onRetry={() => router.reload({ only })} />}
        >
            {children}
        </Deferred>
    );
}
