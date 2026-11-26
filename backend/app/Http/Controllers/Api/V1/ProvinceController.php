<?php

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Resources\ProvinceResource;
use Illuminate\Http\Resources\Json\AnonymousResourceCollection;
use Illuminate\Support\Facades\DB;

/**
 * Public province directory for the location step (§9.1). No sign-in needed.
 */
class ProvinceController extends Controller
{
    public function __invoke(): AnonymousResourceCollection
    {
        return ProvinceResource::collection(
            DB::table('province_directory')->orderBy('name')->orderBy('id')->get(),
        );
    }
}
