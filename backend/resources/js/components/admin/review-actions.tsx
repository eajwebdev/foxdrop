import { ActionDialog } from '@/components/admin/action-dialog';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import type { RouteFormDefinition } from '@/wayfinder';
import { Form } from '@inertiajs/react';
import { Check, X } from 'lucide-react';

/** Approve / reject for anything waiting for review (operator, store and rider applications). */
export function ReviewActions({
    approve,
    reject,
    approveLabel,
    approveHint,
    subject,
}: {
    approve: RouteFormDefinition<'post'>;
    reject: RouteFormDefinition<'post'>;
    approveLabel: string;
    approveHint: string;
    subject: string;
}) {
    return (
        <>
            <ActionDialog
                trigger={
                    <Button variant="outline" className="text-destructive" data-test="reject">
                        <X /> Reject
                    </Button>
                }
                title={`Reject ${subject}?`}
                description="The applicant sees your reason and can fix the problem and send the application again."
                action={reject}
                submitLabel="Reject"
                destructive
                reason="Reason for the applicant"
            />
            <Form {...approve} options={{ preserveScroll: true }}>
                {({ processing }) => (
                    <Button
                        type="submit"
                        disabled={processing}
                        title={approveHint}
                        data-test="approve"
                    >
                        {processing ? <Spinner /> : <Check />}
                        {approveLabel}
                    </Button>
                )}
            </Form>
        </>
    );
}
