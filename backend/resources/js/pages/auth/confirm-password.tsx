import InputError from '@/components/input-error';
import PasswordInput from '@/components/password-input';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Spinner } from '@/components/ui/spinner';
import { store } from '@/routes/password/confirm';
import { Form, Head } from '@inertiajs/react';

export default function ConfirmPassword({ needsCode }: { needsCode: boolean }) {
    return (
        <>
            <Head title="Confirm it's you" />

            <Form {...store.form()} resetOnSuccess className="grid gap-5">
                {({ processing, errors }) => (
                    <>
                        <div className="grid gap-2">
                            <Label htmlFor="confirm_password">Password</Label>
                            <PasswordInput
                                id="confirm_password"
                                name="confirm_password"
                                autoComplete="current-password"
                                autoFocus
                                required
                            />
                            <InputError message={errors.confirm_password} />
                        </div>
                        {needsCode && (
                            <div className="grid gap-2">
                                <Label htmlFor="confirm_code">Authenticator code</Label>
                                <Input
                                    id="confirm_code"
                                    name="confirm_code"
                                    inputMode="numeric"
                                    autoComplete="one-time-code"
                                    maxLength={6}
                                    placeholder="6-digit code"
                                    required
                                />
                                <InputError message={errors.confirm_code} />
                            </div>
                        )}
                        <Button
                            className="w-full"
                            disabled={processing}
                            data-test="confirm-password-button"
                        >
                            {processing && <Spinner />}
                            Confirm
                        </Button>
                    </>
                )}
            </Form>
        </>
    );
}

ConfirmPassword.layout = {
    title: "Confirm it's you",
    description:
        'This area is protected. Enter your password and a code from your authenticator app.',
};
