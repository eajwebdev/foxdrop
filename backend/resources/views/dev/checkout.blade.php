<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>FoxDrop test checkout</title>
    <style>
        body { font-family: system-ui, sans-serif; margin: 0; background: #f6f6f9; color: #05153c; }
        main { max-width: 420px; margin: 0 auto; padding: 32px 20px; }
        .card { background: #fff; border: 1px solid #e3e3ea; border-radius: 16px; padding: 24px; }
        .tag { display: inline-block; background: #fff4d6; color: #7a5600; border-radius: 8px; padding: 4px 10px; font-size: 13px; }
        h1 { font-size: 22px; margin: 16px 0 4px; }
        .amount { font-size: 34px; font-weight: 700; margin: 8px 0 24px; }
        button { width: 100%; border: 0; border-radius: 999px; padding: 16px; font-size: 17px; font-weight: 600; margin-top: 12px; }
        .pay { background: #f2014f; color: #fff; }
        .cancel { background: #fff; color: #05153c; border: 1px solid #c9c9d6; }
        .done { font-size: 18px; margin-top: 16px; }
    </style>
</head>
<body>
<main>
    <div class="card">
        <span class="tag">Test checkout — no real money</span>
        <h1>FoxDrop {{ strtolower($payment->purpose) }} payment</h1>
        <div class="amount">₱{{ number_format($payment->amount_centavos / 100, 2) }}</div>
        @if ($payment->status === 'PENDING')
            <form method="post" action="{{ route('dev.checkout.pay', $payment) }}">@csrf<button class="pay" type="submit">Pay (test)</button></form>
            <form method="post" action="{{ route('dev.checkout.cancel', $payment) }}">@csrf<button class="cancel" type="submit">Cancel</button></form>
        @else
            <p class="done">Payment {{ strtolower($payment->status) }}. You can close this page and go back to the app.</p>
        @endif
    </div>
</main>
</body>
</html>