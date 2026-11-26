import { Button } from '@/components/ui/button';
import admin from '@/routes/admin';
import { Head, Link, setLayoutProps } from '@inertiajs/react';

const TEXT: Record<number, { title: string; description: string }> = {
    403: {
        title: 'Not allowed',
        description:
            'Your admin account does not have permission for this page. Ask the super admin if you need it.',
    },
    404: {
        title: 'Not found',
        description: 'This page or record does not exist, or it was removed.',
    },
    429: { title: 'Too many tries', description: 'Please wait a minute and try again.' },
    500: {
        title: 'Something went wrong',
        description: 'The problem was logged. Please try again in a moment.',
    },
    503: {
        title: 'Back soon',
        description: 'FoxDrop Admin is being updated. Please try again in a few minutes.',
    },
};

export default function ErrorPage({ status, message }: { status: number; message: string | null }) {
    const text = TEXT[status] ?? TEXT[500];
    setLayoutProps({ title: text.title, description: '' });

    return (
        <>
            <Head title={text.title} />
            <div className="space-y-6 text-center">
                <p className="font-heading text-6xl font-bold text-brand">{status}</p>
                <p className="text-sm text-muted-foreground">{message ?? text.description}</p>
                <Button asChild>
                    <Link href={admin.dashboard()}>Back to the dashboard</Link>
                </Button>
            </div>
        </>
    );
}
