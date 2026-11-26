import InputError from '@/components/input-error';
import PasswordInput from '@/components/password-input';
import { Label } from '@/components/ui/label';

/** A new password and its repeat (own password change, new staff account, staff password reset). */
export function NewPasswordFields({
    passwordRules,
    error,
}: {
    passwordRules: string;
    error?: string;
}) {
    return (
        <>
            <div className="grid gap-1.5">
                <Label htmlFor="password">New password</Label>
                <PasswordInput
                    id="password"
                    name="password"
                    autoComplete="new-password"
                    passwordrules={passwordRules}
                    required
                />
                <InputError message={error} />
            </div>
            <div className="grid gap-1.5">
                <Label htmlFor="password_confirmation">Repeat the new password</Label>
                <PasswordInput
                    id="password_confirmation"
                    name="password_confirmation"
                    autoComplete="new-password"
                    required
                />
            </div>
        </>
    );
}
