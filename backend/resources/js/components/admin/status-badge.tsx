import { humanize } from '@/lib/format';
import { cn } from '@/lib/utils';

type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'brand';

/** One colour per meaning across licenses, applications, stores, riders and accounts. */
const TONES: Record<string, Tone> = {
    ACTIVE: 'success',
    APPROVED: 'success',
    PAID: 'success',
    GRACE: 'warning',
    PENDING_PAYMENT: 'warning',
    PAUSED: 'warning',
    SUBMITTED: 'info',
    DOCS_REVIEW: 'info',
    APPLIED: 'info',
    PENDING: 'info',
    SUSPENDED: 'danger',
    REJECTED: 'danger',
    EXPIRED: 'danger',
    DEACTIVATED: 'neutral',
    CLOSED: 'neutral',
    WITHDRAWN: 'neutral',
    AVAILABLE: 'brand',
    DISABLED: 'neutral',
};

const CLASSES: Record<Tone, string> = {
    success: 'bg-success/12 text-success ring-success/25',
    warning: 'bg-warning/12 text-warning ring-warning/25',
    danger: 'bg-destructive/10 text-destructive ring-destructive/25',
    info: 'bg-info/10 text-info ring-info/25',
    neutral: 'bg-muted text-muted-foreground ring-border',
    brand: 'bg-brand/10 text-brand ring-brand/25',
};

export function StatusBadge({
    status,
    label,
    className,
}: {
    status: string;
    label?: string;
    className?: string;
}) {
    return (
        <span
            className={cn(
                'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap ring-1 ring-inset',
                CLASSES[TONES[status] ?? 'neutral'],
                className,
            )}
        >
            <span aria-hidden className="size-1.5 rounded-full bg-current" />
            {label ?? humanize(status)}
        </span>
    );
}
