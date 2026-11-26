import { StatusBadge } from '@/components/admin/status-badge';
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog';
import { humanize } from '@/lib/format';
import { FileImage, ZoomIn } from 'lucide-react';
import { useState } from 'react';

export type KycDocument = { type: string; url: string; status?: string };

/**
 * KYC photos (§20.4 step 4). Served from the admin session with `no-store`; click to view full size.
 */
export function DocumentGallery({ documents }: { documents: KycDocument[] }) {
    const [open, setOpen] = useState<KycDocument | null>(null);

    if (documents.length === 0) {
        return <p className="text-sm text-muted-foreground">No documents were uploaded.</p>;
    }

    return (
        <>
            <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {documents.map((document) => (
                    <li key={document.type} className="overflow-hidden rounded-lg border">
                        <button
                            type="button"
                            onClick={() => setOpen(document)}
                            className="group relative block aspect-[4/3] w-full bg-muted"
                            aria-label={`View ${humanize(document.type)}`}
                        >
                            <img
                                src={document.url}
                                alt={humanize(document.type)}
                                loading="lazy"
                                className="size-full object-cover transition-transform duration-200 group-hover:scale-[1.02]"
                            />
                            <span className="absolute inset-0 flex items-center justify-center bg-navy/0 text-white opacity-0 transition group-hover:bg-navy/40 group-hover:opacity-100">
                                <ZoomIn className="size-6" />
                            </span>
                        </button>
                        <div className="flex items-center justify-between gap-2 px-3 py-2 text-sm">
                            <span className="flex items-center gap-1.5 font-medium">
                                <FileImage className="size-4 text-muted-foreground" />
                                {humanize(document.type)}
                            </span>
                            {document.status && <StatusBadge status={document.status} />}
                        </div>
                    </li>
                ))}
            </ul>
            <Dialog open={open !== null} onOpenChange={(value) => !value && setOpen(null)}>
                <DialogContent className="sm:max-w-4xl">
                    <DialogTitle>{open ? humanize(open.type) : ''}</DialogTitle>
                    {open && (
                        <img
                            src={open.url}
                            alt={humanize(open.type)}
                            className="max-h-[75vh] w-full rounded-md object-contain"
                        />
                    )}
                </DialogContent>
            </Dialog>
        </>
    );
}
