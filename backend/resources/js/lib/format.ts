/** Display helpers. Money arrives as integer centavos and dates as ISO-8601 UTC; both are only formatted here. */

const MANILA = 'Asia/Manila';

const peso = new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' });
const dateFormat = new Intl.DateTimeFormat('en-PH', { timeZone: MANILA, dateStyle: 'medium' });
const dateTimeFormat = new Intl.DateTimeFormat('en-PH', {
    timeZone: MANILA,
    dateStyle: 'medium',
    timeStyle: 'short',
});
const numberFormat = new Intl.NumberFormat('en-PH');

/** 1500000 → "₱15,000.00". Integer division keeps it exact; no floats touch the amount. */
export function formatPeso(centavos: number): string {
    const sign = centavos < 0 ? '-' : '';
    const abs = Math.abs(centavos);
    const whole = Math.trunc(abs / 100);
    const cents = abs % 100;

    return sign + peso.format(whole).replace(/\.00$/, '') + '.' + String(cents).padStart(2, '0');
}

export function formatNumber(value: number): string {
    return numberFormat.format(value);
}

export function formatDate(iso: string | null | undefined): string {
    return iso ? dateFormat.format(new Date(iso)) : '—';
}

export function formatDateTime(iso: string | null | undefined): string {
    return iso ? dateTimeFormat.format(new Date(iso)) : '—';
}

/** Whole days from now until [iso] (negative when past), in Manila calendar days. */
export function daysUntil(iso: string | null | undefined, now: Date = new Date()): number | null {
    if (!iso) {
        return null;
    }
    const day = (date: Date) => {
        const [y, m, d] = new Intl.DateTimeFormat('en-CA', { timeZone: MANILA })
            .format(date)
            .split('-')
            .map(Number);

        return Date.UTC(y, m - 1, d) / 86_400_000;
    };

    return day(new Date(iso)) - day(now);
}

/** "SUSPENDED" → "Suspended", "PENDING_PAYMENT" → "Pending payment", "store_owner" → "Store owner". */
export function humanize(value: string): string {
    const words = value.replace(/[._]/g, ' ').toLowerCase().trim();

    return words.charAt(0).toUpperCase() + words.slice(1);
}

export function initials(name: string | null | undefined): string {
    const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) {
        return '?';
    }

    return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
}
