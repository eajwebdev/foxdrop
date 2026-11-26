import { ActionDialog } from '@/components/admin/action-dialog';
import { Button } from '@/components/ui/button';
import type { RouteFormDefinition } from '@/wayfinder';
import { Ban, CircleCheck, Power } from 'lucide-react';

type Kind = 'store' | 'rider';
type Copy = {
    label: string;
    title: string;
    description: string;
    destructive?: boolean;
    reason?: boolean;
};

/** What each change means to the partner. Which changes are allowed comes from the server (`status_changes`). */
const COPY: Record<Kind, Record<string, Copy>> = {
    store: {
        SUSPENDED: {
            label: 'Suspend',
            title: 'Suspend {name}?',
            description: 'Customers stop seeing the store until it is reinstated.',
            destructive: true,
        },
        ACTIVE: {
            label: 'Reinstate',
            title: 'Reinstate {name}?',
            description: 'The store is live for customers again.',
            reason: false,
        },
        CLOSED: {
            label: 'Close store',
            title: 'Close {name} for good?',
            description: 'A closed store cannot be reopened; the owner would apply again.',
            destructive: true,
        },
    },
    rider: {
        SUSPENDED: {
            label: 'Suspend',
            title: 'Suspend {name}?',
            description: 'They go offline at once and get no jobs until reinstated.',
            destructive: true,
        },
        APPROVED: {
            label: 'Reinstate',
            title: 'Reinstate {name}?',
            description: 'They can go online and take jobs again.',
            reason: false,
        },
        DEACTIVATED: {
            label: 'Deactivate',
            title: 'Deactivate {name} for good?',
            description:
                'A deactivated rider cannot ride for FoxDrop again without a new application.',
            destructive: true,
        },
    },
};

/**
 * Admin override for a live store or rider (DECISIONS 79): suspend, reinstate, close / deactivate. Each change says
 * what the partner will notice and asks for a reason they will read.
 */
export function PartnerStatusActions({
    kind,
    name,
    changes,
    action,
}: {
    kind: Kind;
    name: string;
    changes: string[];
    action: RouteFormDefinition<'post'>;
}) {
    return (
        <>
            {changes.map((to) => {
                const copy = COPY[kind][to];
                if (!copy) {
                    return null;
                }
                const Icon = to === 'SUSPENDED' ? Ban : copy.destructive ? Power : CircleCheck;

                return (
                    <ActionDialog
                        key={to}
                        trigger={
                            <Button
                                variant="outline"
                                className={copy.destructive ? 'text-destructive' : undefined}
                            >
                                <Icon /> {copy.label}
                            </Button>
                        }
                        title={copy.title.replace('{name}', name)}
                        description={copy.description}
                        action={action}
                        hidden={{ status: to }}
                        submitLabel={copy.label}
                        destructive={copy.destructive}
                        reason={copy.reason === false ? undefined : 'Reason (they see this)'}
                        confirmIdentity
                    />
                );
            })}
        </>
    );
}
