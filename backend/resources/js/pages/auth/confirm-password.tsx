import { IdentityFields } from '@/components/confirm-identity-fields';
import { Button } from '@/components/ui/button';
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
                        <IdentityFields errors={errors} needsCode={needsCode} autoFocus />
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
