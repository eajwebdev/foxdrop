import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { initials } from '@/lib/format';
import type { User } from '@/types';

export function UserInfo({ user, showEmail = false }: { user: User; showEmail?: boolean }) {
    const name = user.full_name ?? user.email ?? 'Admin';

    return (
        <>
            <Avatar className="h-8 w-8 overflow-hidden rounded-full">
                <AvatarFallback className="rounded-full bg-brand text-xs font-semibold text-brand-foreground">
                    {initials(name)}
                </AvatarFallback>
            </Avatar>
            <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-medium">{name}</span>
                <span className="truncate text-xs opacity-70">
                    {showEmail ? user.email : user.is_super_admin ? 'Super admin' : 'Admin staff'}
                </span>
            </div>
        </>
    );
}
