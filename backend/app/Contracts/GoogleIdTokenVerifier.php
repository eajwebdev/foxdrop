<?php

namespace App\Contracts;

use App\Exceptions\GoogleSignInException;
use App\Services\Auth\GoogleIdentity;

/**
 * Checks a Google ID token from Credential Manager: signature, issuer, audience, expiry and nonce.
 */
interface GoogleIdTokenVerifier
{
    /**
     * @param  string  $rawNonce  the nonce the app generated; the token carries its SHA-256 hex
     *
     * @throws GoogleSignInException
     */
    public function verify(string $idToken, string $rawNonce): GoogleIdentity;
}
