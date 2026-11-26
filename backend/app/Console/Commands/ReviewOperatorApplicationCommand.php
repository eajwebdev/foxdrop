<?php

namespace App\Console\Commands;

use App\Actions\Licensing\ReviewOperatorApplication;
use App\Models\OperatorApplication;
use Illuminate\Console\Attributes\Description;
use Illuminate\Console\Attributes\Signature;
use Illuminate\Console\Command;
use Symfony\Component\HttpKernel\Exception\HttpException;

/**
 * The admin decision on an operator application until the admin web exists (Phase W1, DECISIONS 55). It runs the
 * same Action the web will call; the audit log records the system as the actor.
 */
#[Signature('foxdrop:operator-application {action : approve or reject} {id : the application id} {--reason= : why it was rejected}')]
#[Description('Approve or reject a provincial operator application (admin)')]
class ReviewOperatorApplicationCommand extends Command
{
    public function handle(ReviewOperatorApplication $review): int
    {
        $action = (string) $this->argument('action');
        if (! in_array($action, ['approve', 'reject'], true)) {
            $this->error('The action must be "approve" or "reject".');

            return self::INVALID;
        }
        $reason = $this->option('reason');
        if ($action === 'reject' && (! is_string($reason) || mb_strlen(trim($reason)) < 5)) {
            $this->error('A rejection needs --reason (at least 5 characters); the applicant sees it.');

            return self::INVALID;
        }

        $application = OperatorApplication::query()->find((string) $this->argument('id'));
        if ($application === null) {
            $this->error('No application with that id.');

            return self::FAILURE;
        }

        try {
            $review(null, $application, $action === 'approve', is_string($reason) ? trim($reason) : null);
        } catch (HttpException $e) {
            $this->error($e->getMessage());

            return self::FAILURE;
        }
        $this->info($action === 'approve'
            ? "Approved {$application->business_name}: the license now waits for payment."
            : "Rejected {$application->business_name}; the province is available again.");

        return self::SUCCESS;
    }
}
