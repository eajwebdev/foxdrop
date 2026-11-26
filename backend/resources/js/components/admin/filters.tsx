import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { humanize } from '@/lib/format';
import { router } from '@inertiajs/react';
import { Search, X } from 'lucide-react';
import type { ReactNode } from 'react';
import { useEffect, useRef, useState } from 'react';

/** Changes one query-string filter and goes back to page 1 (filters live in the URL: §20.5 saved filters). */
export function applyFilter(key: string, value: string | null) {
    const url = new URL(window.location.href);
    url.searchParams.delete('page');
    url.searchParams.set(key, value ?? '');
    router.get(url.pathname + url.search, {}, { preserveState: true, replace: true });
}

export function FilterBar({ children }: { children: ReactNode }) {
    return (
        <div className="flex flex-col gap-2 border-b p-3 sm:flex-row sm:flex-wrap sm:items-center">
            {children}
        </div>
    );
}

export function SearchFilter({
    name = 'q',
    value,
    placeholder,
}: {
    name?: string;
    value: string | null;
    placeholder: string;
}) {
    const [text, setText] = useState(value ?? '');
    const first = useRef(true);

    useEffect(() => {
        if (first.current) {
            first.current = false;

            return;
        }
        const timer = window.setTimeout(() => applyFilter(name, text.trim() || null), 350);

        return () => window.clearTimeout(timer);
    }, [text, name]);

    return (
        <div className="relative w-full sm:w-72">
            <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
                value={text}
                onChange={(event) => setText(event.target.value)}
                placeholder={placeholder}
                className="pr-8 pl-9"
                aria-label={placeholder}
            />
            {text && (
                <Button
                    variant="ghost"
                    size="icon"
                    className="absolute top-1/2 right-1 size-7 -translate-y-1/2"
                    onClick={() => setText('')}
                    aria-label="Clear search"
                >
                    <X />
                </Button>
            )}
        </div>
    );
}

const ALL = '__all__';

export function SelectFilter({
    name,
    value,
    options,
    allLabel,
    label,
}: {
    name: string;
    value: string | null;
    options: (string | { value: string; label: string })[];
    allLabel: string;
    label: string;
}) {
    const items = options.map((option) =>
        typeof option === 'string' ? { value: option, label: humanize(option) } : option,
    );

    return (
        <Select
            value={value ?? ALL}
            onValueChange={(next) => applyFilter(name, next === ALL ? null : next)}
        >
            <SelectTrigger className="w-full sm:w-48" aria-label={label}>
                <SelectValue />
            </SelectTrigger>
            <SelectContent>
                <SelectItem value={ALL}>{allLabel}</SelectItem>
                {items.map((item) => (
                    <SelectItem key={item.value} value={item.value}>
                        {item.label}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}
