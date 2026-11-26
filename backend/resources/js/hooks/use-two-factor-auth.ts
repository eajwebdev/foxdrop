import { qrCode, recoveryCodes, secretKey } from '@/routes/two-factor';
import { useHttp } from '@inertiajs/react';
import { useCallback, useState } from 'react';

export const OTP_MAX_LENGTH = 6;

/** Loads the authenticator QR code, setup key and recovery codes from Fortify's 2FA endpoints. */
export function useTwoFactorAuth() {
    const { submit } = useHttp();
    const [qrCodeSvg, setQrCodeSvg] = useState<string | null>(null);
    const [manualSetupKey, setManualSetupKey] = useState<string | null>(null);
    const [recoveryCodesList, setRecoveryCodesList] = useState<string[]>([]);
    const [errors, setErrors] = useState<string[]>([]);

    const fetchSetupData = useCallback(async (): Promise<void> => {
        setErrors([]);
        try {
            const [{ svg }, { secretKey: key }] = (await Promise.all([
                submit(qrCode()),
                submit(secretKey()),
            ])) as [{ svg: string }, { secretKey: string }];
            setQrCodeSvg(svg);
            setManualSetupKey(key);
        } catch {
            setErrors(['The authenticator setup could not load. Reload the page to try again.']);
        }
    }, [submit]);

    const fetchRecoveryCodes = useCallback(async (): Promise<void> => {
        setErrors([]);
        try {
            setRecoveryCodesList((await submit(recoveryCodes())) as string[]);
        } catch {
            setErrors(['The recovery codes could not load.']);
            setRecoveryCodesList([]);
        }
    }, [submit]);

    return {
        qrCodeSvg,
        manualSetupKey,
        recoveryCodesList,
        errors,
        fetchSetupData,
        fetchRecoveryCodes,
    };
}
