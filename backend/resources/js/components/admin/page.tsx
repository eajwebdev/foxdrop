import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { LucideIcon } from 'lucide-react';
import { AlertTriangle, Inbox, RotateCw } from 'lucide-react';
import { motion, useReducedMotion } from 'motion/react';
import type { ReactNode } from 'react';

/** Page frame: heading, actions, and a short fade/slide in (§20.5; skipped with reduced motion). */
export function Page({
    title,
    description,
    actions,
    children,
}: {
    title: string;
    description?: ReactNode;
    actions?: ReactNode;
    children: ReactNode;
}) {
    const reduceMotion = useReducedMotion();

    return (
        <motion.div
            initial={reduceMotion ? false : { opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-4 md:p-6"
        >
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div className="min-w-0 space-y-1">
                    <h1 className="truncate text-2xl font-semibold tracking-tight">{title}</h1>
                    {description && (
                        <div className="text-sm text-muted-foreground">{description}</div>
                    )}
                </div>
                {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
            </div>
            {children}
        </motion.div>
    );
}

/** A flat card section with a title (flat cards, soft border: no shadows). */
export function Section({
    title,
    description,
    actions,
    children,
    className,
}: {
    title?: string;
    description?: string;
    actions?: ReactNode;
    children: ReactNode;
    className?: string;
}) {
    return (
        <section className={cn('rounded-xl border bg-card', className)}>
            {(title || actions) && (
                <div className="flex flex-wrap items-start justify-between gap-2 border-b px-5 py-4">
                    <div>
                        {title && <h2 className="text-base font-semibold">{title}</h2>}
                        {description && (
                            <p className="text-sm text-muted-foreground">{description}</p>
                        )}
                    </div>
                    {actions}
                </div>
            )}
            <div className="p-5">{children}</div>
        </section>
    );
}

export function EmptyState({
    icon: Icon = Inbox,
    title,
    description,
}: {
    icon?: LucideIcon;
    title: string;
    description?: string;
}) {
    return (
        <div
            className="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center"
            data-test="empty-state"
        >
            <div className="flex size-12 items-center justify-center rounded-full bg-brand/10 text-brand">
                <Icon className="size-6" />
            </div>
            <div className="space-y-1">
                <p className="font-medium">{title}</p>
                {description && (
                    <p className="max-w-sm text-sm text-muted-foreground">{description}</p>
                )}
            </div>
        </div>
    );
}

export function ErrorState({ onRetry }: { onRetry: () => void }) {
    return (
        <div
            className="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center"
            role="alert"
        >
            <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
                <AlertTriangle className="size-6" />
            </div>
            <div className="space-y-1">
                <p className="font-medium">This did not load</p>
                <p className="text-sm text-muted-foreground">
                    Check your connection and try again.
                </p>
            </div>
            <Button variant="outline" onClick={onRetry}>
                <RotateCw /> Try again
            </Button>
        </div>
    );
}

/** Label / value pairs for detail pages. */
export function DetailList({ items }: { items: { label: string; value: ReactNode }[] }) {
    return (
        <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2">
            {items.map((item) => (
                <div key={item.label} className="min-w-0 space-y-1">
                    <dt className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
                        {item.label}
                    </dt>
                    <dd className="text-sm break-words">{item.value ?? '—'}</dd>
                </div>
            ))}
        </dl>
    );
}
