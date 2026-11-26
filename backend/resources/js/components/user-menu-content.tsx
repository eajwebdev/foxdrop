import {
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import { UserInfo } from '@/components/user-info';
import { logout } from '@/routes';
import admin from '@/routes/admin';
import type { User } from '@/types';
import { Link, router } from '@inertiajs/react';
import { LogOut, ShieldCheck } from 'lucide-react';

export function UserMenuContent({ user }: { user: User }) {
    return (
        <>
            <DropdownMenuLabel className="p-0 font-normal">
                <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                    <UserInfo user={user} showEmail={true} />
                </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
                <DropdownMenuItem asChild>
                    <Link className="block w-full cursor-pointer" href={admin.settings.security()}>
                        <ShieldCheck className="mr-2" />
                        My security
                    </Link>
                </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
                <Link
                    className="block w-full cursor-pointer"
                    href={logout()}
                    as="button"
                    onClick={() => router.flushAll()}
                    data-test="logout-button"
                >
                    <LogOut className="mr-2" />
                    Sign out
                </Link>
            </DropdownMenuItem>
        </>
    );
}
