import { describe, expect, it } from 'vitest';
import { daysUntil, formatPeso, humanize, initials } from './format';

describe('formatPeso', () => {
    it('formats centavos without floating point', () => {
        expect(formatPeso(1_500_000)).toBe('₱15,000.00');
        expect(formatPeso(1)).toBe('₱0.01');
        expect(formatPeso(123_456_789)).toBe('₱1,234,567.89');
        expect(formatPeso(-4_050)).toBe('-₱40.50');
        expect(formatPeso(0)).toBe('₱0.00');
    });
});

describe('daysUntil', () => {
    it('counts Manila calendar days', () => {
        const now = new Date('2026-11-26T15:30:00Z'); // 23:30 in Manila
        expect(daysUntil('2026-11-26T16:30:00Z', now)).toBe(1); // 00:30 the next Manila day
        expect(daysUntil('2026-11-20T00:00:00Z', now)).toBe(-6);
        expect(daysUntil(null, now)).toBeNull();
    });
});

describe('humanize and initials', () => {
    it('turns codes into labels', () => {
        expect(humanize('PENDING_PAYMENT')).toBe('Pending payment');
        expect(humanize('store_owner')).toBe('Store owner');
        expect(humanize('license.offline_payment_recorded')).toBe(
            'License offline payment recorded',
        );
    });

    it('takes first and last initials', () => {
        expect(initials('Maria Clara Santos')).toBe('MS');
        expect(initials('Admin')).toBe('A');
        expect(initials(null)).toBe('?');
    });
});
