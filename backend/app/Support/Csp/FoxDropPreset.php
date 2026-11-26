<?php

namespace App\Support\Csp;

use Spatie\Csp\Directive;
use Spatie\Csp\Keyword;
use Spatie\Csp\Policy;
use Spatie\Csp\Preset;

/**
 * Content Security Policy for the web pages (§20.6): scripts only from this app with a per-request nonce (Vite uses
 * it too), nothing framed, no plugins. Inline style *attributes* stay allowed because Radix and motion position
 * popovers and animate with them; they cannot run code.
 */
final class FoxDropPreset implements Preset
{
    public function configure(Policy $policy): void
    {
        $policy
            ->add(Directive::DEFAULT, Keyword::SELF)
            ->add(Directive::BASE, Keyword::SELF)
            ->add(Directive::CONNECT, Keyword::SELF)
            ->add(Directive::FONT, [Keyword::SELF, 'https://fonts.bunny.net'])
            ->add(Directive::FORM_ACTION, Keyword::SELF)
            ->add(Directive::FRAME_ANCESTORS, Keyword::NONE)
            ->add(Directive::IMG, [Keyword::SELF, 'data:', 'blob:'])
            ->add(Directive::OBJECT, Keyword::NONE)
            ->add(Directive::SCRIPT, Keyword::SELF)
            ->addNonce(Directive::SCRIPT)
            ->add(Directive::STYLE, [Keyword::SELF, Keyword::UNSAFE_INLINE, 'https://fonts.bunny.net']);
    }
}
