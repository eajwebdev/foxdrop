<?php

namespace App\Http\Controllers\Admin\Franchises;

use App\Actions\Licensing\AssignProvinceLicense;
use App\Actions\Licensing\ChangeLicenseSuspension;
use App\Actions\Licensing\RecordOfflineLicensePayment;
use App\Http\Controllers\Admin\AdminController;
use App\Http\Requests\Admin\AssignLicenseRequest;
use App\Http\Requests\Admin\ReasonRequest;
use App\Http\Requests\Admin\RecordLicensePaymentRequest;
use App\Models\Province;
use App\Models\ProvinceLicense;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;

/**
 * License changes from the province page (§9.5 step 3, DECISIONS 76–78). Each runs one shared Action.
 */
class LicenseController extends AdminController
{
    public function assign(AssignLicenseRequest $request, Province $province, AssignProvinceLicense $assign): RedirectResponse
    {
        $assign($this->admin($request), $province, $request->operator());

        return $this->done('Province assigned. It goes live when the license is paid.');
    }

    public function recordPayment(RecordLicensePaymentRequest $request, ProvinceLicense $license, RecordOfflineLicensePayment $record): RedirectResponse
    {
        $note = trim($request->string('note')->toString());
        $record($this->admin($request), $license, $request->string('method')->toString(), $request->string('reference')->trim()->toString(), $note === '' ? null : $note);

        return $this->done('Payment recorded and the license updated.');
    }

    public function suspend(ReasonRequest $request, ProvinceLicense $license, ChangeLicenseSuspension $change): RedirectResponse
    {
        $change->suspend($this->admin($request), $license, $request->reason());

        return $this->done('License suspended.');
    }

    public function reinstate(Request $request, ProvinceLicense $license, ChangeLicenseSuspension $change): RedirectResponse
    {
        $change->reinstate($this->admin($request), $license);

        return $this->done('Suspension lifted.');
    }
}
