import InputError from '@/components/input-error';
import PasswordInput from '@/components/password-input';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Spinner } from '@/components/ui/spinner';
import { store } from '@/routes/login';
import { Form, Head } from '@inertiajs/react';

export default function Login({ status }: { status?: string }) {
    return (
        <>
            <Head title="Sign in" />

            {status && (
                <div
                    role="status"
                    className="rounded-lg border bg-muted px-3 py-2 text-center text-sm"
                >
                    {status}
                </div>
            )}

            <Form {...store.form()} resetOnSuccess={['password']} className="flex flex-col gap-6">
                {({ processing, errors }) => (
                    <div className="grid gap-5">
                        <div className="grid gap-2">
                            <Label htmlFor="email">Email address</Label>
                            <Input
                                id="email"
                                type="email"
                                name="email"
                                required
                                autoFocus
                                autoComplete="username"
                                placeholder="you@foxdrop.ph"
                            />
                            <InputError message={errors.email} />
                        </div>

                        <div className="grid gap-2">
                            <Label htmlFor="password">Password</Label>
                            <PasswordInput
                                id="password"
                                name="password"
                                required
                                autoComplete="current-password"
                                placeholder="Password"
                            />
                            <InputError message={errors.password} />
                        </div>

                        <Button
                            type="submit"
                            className="mt-2 w-full"
                            disabled={processing}
                            data-test="login-button"
                        >
                            {processing && <Spinner />}
                            Sign in
                        </Button>

                        <p className="text-center text-xs text-muted-foreground">
                            Admin accounts only. Forgot your password? Ask the super admin to reset
                            it.
                        </p>
                    </div>
                )}
            </Form>
        </>
    );
}

Login.layout = {
    title: 'FoxDrop Admin',
    description: 'Sign in to manage provinces, partners and users.',
};
