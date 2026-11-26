import { ConfirmIdentityFields } from '@/components/confirm-identity-fields';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Spinner } from '@/components/ui/spinner';
import { Textarea } from '@/components/ui/textarea';
import type { RouteFormDefinition } from '@/wayfinder';
import { Form } from '@inertiajs/react';
import type { ReactNode } from 'react';
import { useState } from 'react';

type FieldsRender = (errors: Record<string, string>) => ReactNode;

/**
 * Every admin change goes through a dialog that says what will happen (§20.5 "confirm dialogs"), asks for a reason
 * when the affected person will read one, and for "confirm it's you" when the action is dangerous.
 */
export function ActionDialog({
    trigger,
    title,
    description,
    action,
    submitLabel,
    destructive = false,
    reason,
    confirmIdentity = false,
    fields,
    hidden,
}: {
    trigger: ReactNode;
    title: string;
    description: ReactNode;
    /** A Wayfinder form target, e.g. `admin.licenses.suspend.form(id)`. */
    action: RouteFormDefinition<'post'>;
    submitLabel: string;
    destructive?: boolean;
    /** Label for a required reason (5–300 characters), shown to the person affected. */
    reason?: string;
    confirmIdentity?: boolean;
    fields?: FieldsRender;
    hidden?: Record<string, string>;
}) {
    const [open, setOpen] = useState(false);

    return (
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>{trigger}</DialogTrigger>
            <DialogContent className="sm:max-w-md">
                <Form
                    {...action}
                    options={{ preserveScroll: true }}
                    onSuccess={() => setOpen(false)}
                    resetOnSuccess
                    className="space-y-4"
                >
                    {({ processing, errors }) => (
                        <>
                            <DialogHeader>
                                <DialogTitle>{title}</DialogTitle>
                                <DialogDescription>{description}</DialogDescription>
                            </DialogHeader>
                            {hidden &&
                                Object.entries(hidden).map(([name, value]) => (
                                    <input key={name} type="hidden" name={name} value={value} />
                                ))}
                            {fields?.(errors)}
                            {reason && (
                                <div className="grid gap-1.5">
                                    <Label htmlFor="reason">{reason}</Label>
                                    <Textarea
                                        id="reason"
                                        name="reason"
                                        minLength={5}
                                        maxLength={300}
                                        required
                                        rows={3}
                                    />
                                    <InputError message={errors.reason} />
                                </div>
                            )}
                            {confirmIdentity && <ConfirmIdentityFields errors={errors} />}
                            <InputError message={errors.status} />
                            <DialogFooter className="gap-2">
                                <DialogClose asChild>
                                    <Button type="button" variant="outline">
                                        Cancel
                                    </Button>
                                </DialogClose>
                                <Button
                                    type="submit"
                                    variant={destructive ? 'destructive' : 'default'}
                                    disabled={processing}
                                >
                                    {processing && <Spinner />}
                                    {submitLabel}
                                </Button>
                            </DialogFooter>
                        </>
                    )}
                </Form>
            </DialogContent>
        </Dialog>
    );
}
