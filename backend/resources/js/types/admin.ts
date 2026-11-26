/** Shapes the admin pages share (App\Queries\Admin\Present on the server). */
export type Person = {
    id: string;
    full_name: string | null;
    email: string | null;
    phone: string | null;
};

export type ProvinceRef = { id: string; name: string | null };

export type Option = { value: string; label: string };
