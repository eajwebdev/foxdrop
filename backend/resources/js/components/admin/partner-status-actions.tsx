import { ActionDialog } from '@/components/admin/action-dialog';
import { Button } from '@/components/ui/button';
import type { RouteFormDefinition } from '@/wayfinder';
import { Ban, CircleCheck, Power } from 'lucide-react';

type Change = {
    to: string;
    label: string;
    title: string;
    description: string;
    destructive?: boolean;
    reason?: boolean;
};

/**
 * Admin override for a live store or rider (DECISIONS 79): suspend, reinstate, close / deactivate. Each change says
 * what the partner will notice and asks for a reason they will read.
 */
export function PartnerStatusActions({
    action,
    changes,
}: {
    action: RouteFormDefinition<'post'>;
    changes: Change[];
}) {
    return (
        <>
            {changes.map((change) => {
                const Icon =
                    change.to === 'SUSPENDED' ? Ban : change.destructive ? Power : CircleCheck;

                return (
                    <ActionDialog
                        key={change.to}
                        trigger={
                            <Button
                                variant="outline"
                                className={change.destructive ? 'text-destructive' : undefined}
                            >
                                <Icon /> {change.label}
                            </Button>
                        }
                        title={change.title}
                        description={change.description}
                        action={action}
                        hidden={{ status: change.to }}
                        submitLabel={change.label}
                        destructive={change.destructive}
                        reason={change.reason === false ? undefined : 'Reason (they see this)'}
                        confirmIdentity
                    />
                );
            })}
        </>
    );
}
