import { router } from '@inertiajs/react';
import { useEffect } from 'react';

/** After this long without a key press, click or pointer move, refreshing stops. */
const IDLE_AFTER_MS = 5 * 60_000;

/**
 * Reloads [only] every [intervalMs] while the admin is using the page. An open but untouched tab stops refreshing,
 * so the 30-minute idle session timeout (§20.4) still signs it out. Refreshing resumes on the next interaction.
 */
export function usePollWhileActive(intervalMs: number, only: string[]): void {
    const key = only.join(',');

    useEffect(() => {
        let lastActive = Date.now();
        const markActive = () => {
            lastActive = Date.now();
        };
        const events = ['pointermove', 'keydown', 'pointerdown', 'scroll'] as const;
        events.forEach((event) => window.addEventListener(event, markActive, { passive: true }));

        const timer = window.setInterval(() => {
            if (document.visibilityState === 'visible' && Date.now() - lastActive < IDLE_AFTER_MS) {
                router.reload({ only: key.split(',') });
            }
        }, intervalMs);

        return () => {
            window.clearInterval(timer);
            events.forEach((event) => window.removeEventListener(event, markActive));
        };
    }, [intervalMs, key]);
}
