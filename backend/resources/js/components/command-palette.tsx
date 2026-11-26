import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { adminNav } from '@/lib/navigation';
import admin from '@/routes/admin';
import { router, usePage } from '@inertiajs/react';
import { Command } from 'cmdk';
import { Search } from 'lucide-react';
import { useEffect, useState } from 'react';

type Entry = { label: string; hint?: string; href: string };

/**
 * ⌘K / Ctrl+K (§20.5): jump to any section, or search users, stores and riders by what was typed.
 */
export function CommandPalette() {
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState('');
    const user = usePage().props.auth.user;
    const can = (permission?: string) =>
        !permission || (user?.permissions as string[] | undefined)?.includes(permission);

    useEffect(() => {
        const onKey = (event: KeyboardEvent) => {
            if (event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey)) {
                event.preventDefault();
                setOpen((value) => !value);
            }
        };
        window.addEventListener('keydown', onKey);

        return () => window.removeEventListener('keydown', onKey);
    }, []);

    const sections: Entry[] = adminNav
        .filter((item) => item.href && can(item.permission))
        .map((item) => ({ label: item.title, href: item.href as string }));
    const term = query.trim();
    const searches: Entry[] = term
        ? [
              can('users.view') && {
                  label: `Users matching “${term}”`,
                  href: admin.users.index.url({ query: { q: term } }),
              },
              can('partners.view') && {
                  label: `Stores matching “${term}”`,
                  href: admin.stores.index.url({ query: { q: term, status: '' } }),
              },
              can('partners.view') && {
                  label: `Riders matching “${term}”`,
                  href: admin.riders.index.url({ query: { q: term, status: '' } }),
              },
          ].filter((entry): entry is Entry => Boolean(entry))
        : [];

    const go = (href: string) => {
        setOpen(false);
        setQuery('');
        router.visit(href);
    };

    return (
        <>
            <Button
                variant="outline"
                className="h-9 w-full max-w-64 justify-start gap-2 text-muted-foreground sm:w-64"
                onClick={() => setOpen(true)}
            >
                <Search className="size-4" />
                <span className="flex-1 text-left">Search or jump to…</span>
                <kbd className="hidden rounded border bg-muted px-1.5 font-mono text-[10px] sm:inline">
                    Ctrl K
                </kbd>
            </Button>
            <Dialog open={open} onOpenChange={setOpen}>
                <DialogContent className="overflow-hidden p-0 sm:max-w-lg">
                    <DialogTitle className="sr-only">Search or jump to</DialogTitle>
                    <Command
                        label="Command palette"
                        className="[&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:py-2 [&_[cmdk-group-heading]]:text-xs [&_[cmdk-group-heading]]:text-muted-foreground"
                    >
                        <div className="flex items-center gap-2 border-b px-3">
                            <Search className="size-4 text-muted-foreground" />
                            <Command.Input
                                value={query}
                                onValueChange={setQuery}
                                placeholder="Type a page, name, phone or email…"
                                className="h-11 w-full bg-transparent text-sm outline-none"
                            />
                        </div>
                        <Command.List className="max-h-80 overflow-y-auto p-1">
                            <Command.Empty className="p-4 text-center text-sm text-muted-foreground">
                                Nothing found.
                            </Command.Empty>
                            {searches.length > 0 && (
                                <Command.Group heading="Search">
                                    {searches.map((entry) => (
                                        <PaletteItem key={entry.href} entry={entry} onSelect={go} />
                                    ))}
                                </Command.Group>
                            )}
                            <Command.Group heading="Go to">
                                {sections.map((entry) => (
                                    <PaletteItem key={entry.href} entry={entry} onSelect={go} />
                                ))}
                            </Command.Group>
                        </Command.List>
                    </Command>
                </DialogContent>
            </Dialog>
        </>
    );
}

function PaletteItem({ entry, onSelect }: { entry: Entry; onSelect: (href: string) => void }) {
    return (
        <Command.Item
            value={entry.label}
            onSelect={() => onSelect(entry.href)}
            className="cursor-pointer rounded-md px-3 py-2 text-sm aria-selected:bg-accent aria-selected:text-accent-foreground"
        >
            {entry.label}
        </Command.Item>
    );
}
