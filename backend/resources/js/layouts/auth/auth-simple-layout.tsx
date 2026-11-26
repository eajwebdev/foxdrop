import type { AuthLayoutProps } from '@/types';

/** Sign-in pages: the FoxDrop mark on a calm card, navy band on wide screens (§20.5). */
export default function AuthSimpleLayout({ children, title, description }: AuthLayoutProps) {
    return (
        <div className="grid min-h-svh lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
            <aside className="relative hidden flex-col justify-between overflow-hidden bg-navy p-10 text-white lg:flex">
                <img
                    src="/images/foxdrop_wordmark_on_dark.webp"
                    alt="FoxDrop"
                    className="h-16 w-auto self-start"
                />
                <div className="space-y-3">
                    <p className="font-heading text-3xl leading-tight font-semibold">
                        Platform control for every province.
                    </p>
                    <p className="max-w-md text-sm text-white/70">
                        Franchises, partners and users, in one place. Every change is checked,
                        confirmed and audited.
                    </p>
                </div>
                <div
                    aria-hidden
                    className="absolute -right-24 -bottom-24 size-80 rounded-full bg-brand/25 blur-3xl"
                />
            </aside>
            <main className="flex items-center justify-center bg-background p-6 md:p-10">
                <div className="w-full max-w-sm space-y-8">
                    <div className="flex flex-col items-center gap-4 text-center">
                        <img
                            src="/images/foxdrop_mark.webp"
                            alt=""
                            className="h-14 w-auto dark:hidden"
                        />
                        <img
                            src="/images/foxdrop_mark_on_dark.webp"
                            alt=""
                            className="hidden h-14 w-auto dark:block"
                        />
                        <div className="space-y-1.5">
                            <h1 className="text-2xl font-semibold">{title}</h1>
                            {description && (
                                <p className="text-sm text-muted-foreground">{description}</p>
                            )}
                        </div>
                    </div>
                    {children}
                </div>
            </main>
        </div>
    );
}
