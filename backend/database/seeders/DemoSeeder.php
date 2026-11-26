<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Crypt;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use RuntimeException;

/**
 * LOCAL DEVELOPMENT ONLY demo data (§13 Phase 1): Negros Occidental with an ACTIVE license, 1 operator, 3 stores with
 * products, 3 riders, 2 customers and 1 super admin. Sign in with the demo numbers below; their codes come from
 * FOXDROP_TEST_OTPS in .env (123456 by default in .env.example).
 *
 *   +63 917 000 0001  Juan Dela Cruz      customer
 *   +63 917 000 0002  Maria Santos        customer
 *   +63 917 000 0003  Paolo Reyes         rider
 *   +63 917 000 0004  Carlo Mendoza       rider
 *   +63 917 000 0005  Ana Villanueva      rider
 *   +63 917 000 0006  Liza Gonzaga        operator, Negros Occidental
 *   +63 917 000 0007  Rosa Lacson         store owner, Mama's Kitchen
 *   +63 917 000 0008  Ramon Yulo          store owner, QuickMart
 *   +63 917 000 0009  Teresa Montelibano  store owner, Bacolod Botika
 *   +63 917 000 0011  Nena Villareal      applied to open "Lola Nena's Bakery" (waiting for the operator)
 *   +63 917 000 0012  Jomar Tan           applied as a rider (waiting for the operator)
 *   admin@foxdrop.local / foxdrop-local-admin   super admin (web dashboard only)
 */
class DemoSeeder extends Seeder
{
    public const string NEGROS_OCCIDENTAL = '0604500000';

    public function run(): void
    {
        if (! app()->environment(['local', 'testing'])) {
            throw new RuntimeException('DemoSeeder is for local development only.');
        }

        DB::transaction(function (): void {
            $neg = $this->province();
            $this->users();
            $this->licenseAndTerritory($neg);
            $this->storesAndProducts($neg);
            $this->riders($neg);
            $this->addresses($neg);
            $this->wallets($neg);
            $this->applicationsToReview($neg);
        });
    }

    public static function userId(int $n): string
    {
        return sprintf('00000000-0000-4000-8000-%012d', $n);
    }

    public static function storeId(int $n): string
    {
        return sprintf('00000000-0000-4000-a000-%012d', $n);
    }

    public static function riderId(int $n): string
    {
        return sprintf('00000000-0000-4000-c000-%012d', $n);
    }

    private function province(): string
    {
        DB::table('provinces')->where('psgc_code', self::NEGROS_OCCIDENTAL)->update([
            'short_code' => 'NEG',
            'center' => $this->point(122.9509, 10.6765),
        ]);

        return (string) DB::table('provinces')->where('psgc_code', self::NEGROS_OCCIDENTAL)->value('id');
    }

    private function users(): void
    {
        $names = [
            1 => 'Juan Dela Cruz', 2 => 'Maria Santos', 3 => 'Paolo Reyes', 4 => 'Carlo Mendoza', 5 => 'Ana Villanueva',
            6 => 'Liza Gonzaga', 7 => 'Rosa Lacson', 8 => 'Ramon Yulo', 9 => 'Teresa Montelibano',
            11 => 'Nena Villareal', 12 => 'Jomar Tan',
        ];
        $rows = collect($names)->map(fn (string $name, int $n): array => [
            'id' => self::userId($n),
            'full_name' => $name,
            'phone' => sprintf('+6391700000%02d', $n),
            'phone_verified_at' => now(),
            'email' => null,
            'email_verified_at' => null,
            'password' => null,
        ])->values()->all();
        $rows[] = [
            'id' => self::userId(10),
            'full_name' => 'FoxDrop Admin',
            'phone' => null,
            'phone_verified_at' => null,
            'email' => 'admin@foxdrop.local',
            'email_verified_at' => now(),
            'password' => Hash::make('foxdrop-local-admin'),
        ];
        DB::table('users')->insertOrIgnore($rows);

        DB::table('user_roles')->insertOrIgnore(['user_id' => self::userId(10), 'role' => 'super_admin']);
    }

    private function licenseAndTerritory(string $neg): void
    {
        $operator = self::userId(6);
        DB::table('user_roles')->insertOrIgnore(['user_id' => $operator, 'role' => 'operator', 'province_id' => $neg]);

        $applicationId = '00000000-0000-4000-9000-000000000001';
        DB::table('operator_applications')->insertOrIgnore([
            'id' => $applicationId,
            'applicant_user_id' => $operator,
            'province_id' => $neg,
            'business_name' => 'Gonzaga Logistics',
            'registration_number' => 'DTI-0604-000123',
            'contact_phone' => '+639170000006',
            'status' => 'APPROVED',
            'reviewed_by' => self::userId(10),
            'reviewed_at' => now()->subDays(31),
        ]);
        DB::table('province_licenses')->insertOrIgnore([
            'id' => '00000000-0000-4000-9000-000000000002',
            'province_id' => $neg,
            'operator_user_id' => $operator,
            'application_id' => $applicationId,
            'status' => 'ACTIVE',
            'start_at' => now()->subDays(30),
            'end_at' => now()->addDays(335),
            'fee_centavos' => 1_500_000,
        ]);

        if (DB::table('delivery_rate_cards')->where('province_id', $neg)->doesntExist()) {
            DB::table('delivery_rate_cards')->insert(['province_id' => $neg, 'created_by' => $operator, 'effective_from' => now()->subDays(30)]);
            DB::table('delivery_zones')->insert([
                'province_id' => $neg,
                'name' => 'Metro Bacolod',
                'area' => DB::raw("ST_GeomFromText('MULTIPOLYGON(((122.88 10.58, 123.08 10.58, 123.08 10.82, 122.88 10.82, 122.88 10.58)))', 4326)::geography"),
                'created_by' => $operator,
            ]);
        }
    }

    private function storesAndProducts(string $neg): void
    {
        $hours = fn (string $open, string $close, bool $sunday = true): string => json_encode(collect(['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'])
            ->reject(fn (string $day): bool => $day === 'sun' && ! $sunday)
            ->mapWithKeys(fn (string $day): array => [$day => [[$open, $close]]])
            ->all(), JSON_THROW_ON_ERROR);

        $stores = [
            [1, 7, "Mama's Kitchen", 'FOOD', 'Home-style burgers, fries and inasal.', 122.9530, 10.6740, 'Lacson St.', 'Barangay 17', $hours('08:00', '21:00'), 15, 4.8, 124],
            [2, 8, 'QuickMart', 'GROCERIES', 'Groceries and household essentials.', 122.9600, 10.6800, 'Araneta Ave.', 'Singcang', $hours('07:00', '22:00'), 20, 4.7, 88],
            [3, 9, 'Bacolod Botika', 'MEDICINE', 'Pharmacy: over-the-counter medicine and health essentials.', 122.9450, 10.6690, 'Burgos St.', 'Villamonte', $hours('08:00', '20:00', false), 10, 4.9, 41],
        ];
        foreach ($stores as [$n, $owner, $name, $category, $description, $lng, $lat, $street, $barangay, $openingHours, $prep, $rating, $ratingCount]) {
            DB::table('stores')->insertOrIgnore([
                'id' => self::storeId($n),
                'province_id' => $neg,
                'owner_user_id' => self::userId($owner),
                'name' => $name,
                'category' => $category,
                'description' => $description,
                'location' => $this->point($lng, $lat),
                'address_line' => $street,
                'barangay' => $barangay,
                'city' => 'Bacolod City',
                'phone' => sprintf('+6391700000%02d', $owner),
                'opening_hours' => $openingHours,
                'prep_time_min' => $prep,
                'status' => 'ACTIVE',
                'rating_avg' => $rating,
                'rating_count' => $ratingCount,
            ]);
            DB::table('user_roles')->insertOrIgnore([
                'user_id' => self::userId($owner), 'role' => 'store_owner', 'province_id' => $neg, 'store_id' => self::storeId($n),
            ]);
        }

        $categories = [
            ['00000000-0000-4000-b000-000000000001', 1, 'Burgers & Mains', 1],
            ['00000000-0000-4000-b000-000000000002', 1, 'Sides & Drinks', 2],
            ['00000000-0000-4000-b000-000000000003', 2, 'Pantry', 1],
            ['00000000-0000-4000-b000-000000000004', 3, 'Medicine', 1],
        ];
        foreach ($categories as [$id, $store, $name, $sort]) {
            DB::table('product_categories')->insertOrIgnore(['id' => $id, 'store_id' => self::storeId($store), 'province_id' => $neg, 'name' => $name, 'sort_order' => $sort]);
        }

        if (DB::table('products')->where('province_id', $neg)->exists()) {
            return;
        }
        $addOns = '[{"group":"Add-ons","required":false,"max":2,"choices":[{"name":"Extra cheese","price_centavos":2000},{"name":"Bacon","price_centavos":3500}]}]';
        $products = [
            [1, 1, 'Cheeseburger', 'Quarter-pound beef patty with cheese.', 12900, 1, $addOns],
            [1, 1, 'Chicken Inasal', 'Grilled Bacolod-style chicken with garlic rice.', 15900, 2, '[]'],
            [1, 2, 'Fries (Regular)', null, 4900, 3, '[]'],
            [1, 2, 'Iced Tea', null, 3900, 4, '[]'],
            [2, 3, 'Rice 5 kg', 'Well-milled rice.', 27500, 1, '[]'],
            [2, 3, 'Eggs (1 dozen)', null, 9600, 2, '[]'],
            [2, 3, 'Bottled Water 1.5 L', null, 3500, 3, '[]'],
            [3, 4, 'Paracetamol 500 mg (10 tablets)', null, 4500, 1, '[]'],
            [3, 4, 'Vitamin C 500 mg (30 tablets)', null, 18000, 2, '[]'],
            [3, 4, 'Face Masks (box of 50)', null, 15000, 3, '[]'],
        ];
        DB::table('products')->insert(array_map(fn (array $p): array => [
            'id' => (string) Str::uuid7(),
            'store_id' => self::storeId($p[0]),
            'province_id' => $neg,
            'category_id' => sprintf('00000000-0000-4000-b000-%012d', $p[1]),
            'name' => $p[2],
            'description' => $p[3],
            'price_centavos' => $p[4],
            'sort_order' => $p[5],
            'options' => $p[6],
        ], $products));
    }

    private function riders(string $neg): void
    {
        $riders = [[1, 3, 'MOTORCYCLE', 'NEG 1234', 'A001', 400, 20, 4.9, 328], [2, 4, 'MOTORCYCLE', 'NEG 5678', 'A002', 200, 18, 4.7, 112], [3, 5, 'BICYCLE', null, 'A003', 300, 10, 4.8, 57]];
        foreach ($riders as [$n, $user, $vehicle, $plate, $last4, $expiryDays, $approvedDaysAgo, $rating, $ratingCount]) {
            DB::table('riders')->insertOrIgnore([
                'id' => self::riderId($n),
                'user_id' => self::userId($user),
                'province_id' => $neg,
                'vehicle_type' => $vehicle,
                'plate_no' => $plate,
                'license_no_last4' => $last4,
                'license_expiry' => now()->addDays($expiryDays)->toDateString(),
                'status' => 'APPROVED',
                'approved_by' => self::userId(6),
                'approved_at' => now()->subDays($approvedDaysAgo),
                'rating_avg' => $rating,
                'rating_count' => $ratingCount,
            ]);
            DB::table('user_roles')->insertOrIgnore(['user_id' => self::userId($user), 'role' => 'rider', 'province_id' => $neg]);
        }
    }

    private function addresses(string $neg): void
    {
        if (DB::table('addresses')->whereIn('user_id', [self::userId(1), self::userId(2)])->exists()) {
            return;
        }
        DB::table('addresses')->insert([
            ['user_id' => self::userId(1), 'province_id' => $neg, 'label' => 'Home', 'location' => $this->point(122.9700, 10.6900), 'address_line' => '12 Rosario St.', 'landmark' => 'Near the chapel', 'barangay' => 'Taculing', 'city' => 'Bacolod City'],
            ['user_id' => self::userId(2), 'province_id' => $neg, 'label' => 'Home', 'location' => $this->point(122.9400, 10.6600), 'address_line' => '45 Mabini St.', 'landmark' => null, 'barangay' => 'Villamonte', 'city' => 'Bacolod City'],
        ]);
    }

    private function wallets(string $neg): void
    {
        $operator = self::userId(6);
        $rows = [
            ['owner_type' => 'PLATFORM', 'owner_id' => null, 'province_id' => null, 'kind' => 'PLATFORM_REVENUE'],
            ['owner_type' => 'SYSTEM', 'owner_id' => null, 'province_id' => null, 'kind' => 'GATEWAY_CLEARING'],
            ['owner_type' => 'SYSTEM', 'owner_id' => null, 'province_id' => null, 'kind' => 'CUSTOMER_REFUND'],
            ['owner_type' => 'OPERATOR', 'owner_id' => $operator, 'province_id' => $neg, 'kind' => 'OPERATOR_REVENUE'],
            ['owner_type' => 'OPERATOR', 'owner_id' => $operator, 'province_id' => $neg, 'kind' => 'OPERATOR_CASH'],
        ];
        foreach ([1, 2, 3] as $n) {
            foreach (['STORE_TOPUP', 'STORE_PAYABLE'] as $kind) {
                $rows[] = ['owner_type' => 'STORE', 'owner_id' => self::storeId($n), 'province_id' => $neg, 'kind' => $kind];
            }
            foreach (['RIDER_EARNINGS', 'RIDER_CASH'] as $kind) {
                $rows[] = ['owner_type' => 'RIDER', 'owner_id' => self::riderId($n), 'province_id' => $neg, 'kind' => $kind];
            }
        }
        DB::table('wallets')->insertOrIgnore($rows);
    }

    /**
     * Phase 2: one store and one rider waiting for the operator, with generated document photos, so the operator
     * demo (0006) has approvals to review.
     */
    private function applicationsToReview(string $neg): void
    {
        $storeId = self::storeId(11);
        DB::table('stores')->insertOrIgnore([
            'id' => $storeId,
            'province_id' => $neg,
            'owner_user_id' => self::userId(11),
            'name' => "Lola Nena's Bakery",
            'category' => 'FOOD',
            'description' => 'Pandesal, ensaymada and piaya baked every morning.',
            'location' => $this->point(122.9580, 10.6720),
            'address_line' => 'Gatuslao St.',
            'barangay' => 'Barangay 8',
            'city' => 'Bacolod City',
            'phone' => '+639170000011',
            'status' => 'SUBMITTED',
        ]);
        foreach (['BUSINESS_PERMIT' => 'Business permit', 'VALID_ID' => 'Valid ID'] as $type => $label) {
            DB::table('store_documents')->insertOrIgnore([
                'store_id' => $storeId, 'province_id' => $neg, 'doc_type' => $type,
                'storage_path' => $this->samplePhoto("stores/{$storeId}/".strtolower($type).'.jpg', $label),
            ]);
        }

        $riderId = self::riderId(12);
        DB::table('riders')->insertOrIgnore([
            'id' => $riderId,
            'user_id' => self::userId(12),
            'province_id' => $neg,
            'vehicle_type' => 'MOTORCYCLE',
            'plate_no' => 'NEG 9012',
            'license_no_encrypted' => Crypt::encryptString('N0123456789'),
            'license_no_last4' => '6789',
            'license_expiry' => now()->addYears(2)->toDateString(),
            'status' => 'DOCS_REVIEW',
        ]);
        $documents = ['VALID_ID' => 'Valid ID', 'DRIVERS_LICENSE' => "Driver's license", 'SELFIE' => 'Selfie', 'OR_CR' => 'OR/CR', 'VEHICLE_PHOTO' => 'Vehicle'];
        foreach ($documents as $type => $label) {
            DB::table('rider_documents')->insertOrIgnore([
                'rider_id' => $riderId, 'province_id' => $neg, 'doc_type' => $type,
                'storage_path' => $this->samplePhoto("riders/{$riderId}/".strtolower($type).'.jpg', $label),
            ]);
        }
    }

    /**
     * A plain placeholder photo on the private document disk.
     */
    private function samplePhoto(string $path, string $label): string
    {
        if (! Storage::disk('kyc')->exists($path)) {
            $image = imagecreatetruecolor(800, 500);
            imagefill($image, 0, 0, (int) imagecolorallocate($image, 245, 245, 250));
            imagerectangle($image, 20, 20, 779, 479, (int) imagecolorallocate($image, 242, 1, 79));
            imagestring($image, 5, 40, 40, 'DEMO - '.$label, (int) imagecolorallocate($image, 5, 21, 60));
            ob_start();
            imagejpeg($image, null, 80);
            Storage::disk('kyc')->put($path, (string) ob_get_clean());
            imagedestroy($image);
        }

        return $path;
    }

    /**
     * A WGS84 point as EWKT; PostGIS reads it straight into a geography column.
     */
    private function point(float $lng, float $lat): string
    {
        return sprintf('SRID=4326;POINT(%F %F)', $lng, $lat);
    }
}
