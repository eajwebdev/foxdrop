# Phase 3 plan — Catalog & customer ordering

Source: MASTER_PROMPT §13 (Phase 3), §5.4, §6, §7, §9.1 steps 2–7, §9.3, §12; `reference/workflows/01_Customer_Workflow.png`.
Decisions 83–92.

## Backend (`backend/`)
1. **Schema 16** (forward-only):
   - order-code sequence and per-province short codes
   - `quotes.used_at` (one order per quote)
   - `orders.idempotency_key` (unique per customer)
   - product image re-encoding on the public disk
   - `payments.refunded_at`
   - indexes for the store order board and the customer catalog
2. **Store catalog** (store team, their own store only): categories CRUD; products CRUD with photo, price, options,
   availability and stock. Store profile:
   - open / pause (ACTIVE ⇄ PAUSED)
   - prep time
   - accepts COD / pick-up
3. **Customer browse** (published stores of serviceable provinces):
   - `stores?lat&lng&category&q`: distance, ETA range and open/paused, nearest first
   - `stores/{id}`: categories, products and options
   - `search?q`: stores and products
4. **Addresses**: customer CRUD. The province comes from the active delivery zone that contains the pin; outside every
   zone the answer is "Not yet available in your area" (DECISIONS 70, 84).
5. **Quote** (`POST quotes`, `CalculateQuote` Action):
   - Checks:
     - store open and published
     - items belong to it and are available
     - options valid
     - stock
     - delivery address inside a zone of the store's province and within `max_km`
   - Distance comes from a `DistanceProvider`: Google Routes API when a key is set, straight line × 1.25 otherwise
     (DECISIONS 85).
   - Fee and splits follow §6 (`DeliveryFeeCalculator`, commissions half-up, remainder to the operator).
   - Promo codes: PERCENT, FIXED, FREE_DELIVERY, with owner, limits and window.
   - Valid for 10 minutes.
6. **Place order** (`POST orders`, `PlaceOrder` Action):
   - Takes the `quote_id` only, with an `Idempotency-Key` header.
   - Snapshots the items; codes are a 4-digit pick-up code (PICKUP) or delivery OTP (DELIVERY).
   - Reserves stock.
   - Payment:
     - COD: PLACED at once.
     - ONLINE: PENDING_PAYMENT, plus a `payments` row (purpose ORDER) and a checkout URL.
     - Pick-up orders may also pay at the store (COD).
7. **`OrderTransition` Action**: the one state machine (§5.4).
   - Each step has a table of who may do it.
   - Takes the row lock, sets milestone timestamps and writes `order_events`.
   - Phase 3 covers the steps up to READY_FOR_PICKUP and the pick-up COMPLETED step (store enters the 4-digit code),
     plus customer and store cancellations.
8. **Order payments**:
   - `OrderPaymentHandler` (PaymentPurposeHandler): paid → PLACED; failed or expired → CANCELLED.
   - Refunds go through `PaymentProvider::refund()` (PayMongo Refunds API; the fake provider refunds at once).
   - A refund is recorded on `payments` and the order's `payment_status = REFUNDED`.
9. **Jobs** (every minute): online orders unpaid after 15 min → CANCELLED; PLACED orders unanswered after 5 min →
   CANCELLED, with a refund if paid. Times come from `platform_settings`.
10. **APIs**:
    - Customer orders: list, detail with timeline and codes, cancel, payment status.
    - Store order board: New / Preparing / Ready / Done; accept with prep time, reject with reason, preparing,
      ready, complete a pick-up with its code.

## Android
- **Customer**:
  - `feature:customer-home`: "Deliver to", search, category chips, nearby stores, coming-soon state.
  - `feature:store`: store page and product sheet with options, quantity and add-to-cart.
  - `feature:cart`: one store; asks before clearing when another store's item is added.
  - `feature:checkout`:
    - fulfillment choice
    - address picker and new address from location
    - summary from the server quote
    - promo code, payment method and notes
    - Place Order, then the Custom Tab for online payment
  - `feature:orders`: history, and detail with the timeline (polling), codes and cancel.
- **Store** (business app):
  - `feature:store-orders`: an incoming-order alarm with looping sound and vibration while the app is open
    (DECISIONS 88), the order board, and pick-up code entry.
  - `feature:store-catalog`: categories and products CRUD, with photos.
  - Store home tabs: Orders, Catalog, Store (pause, prep time).
- **Core**:
  - domain models and repositories (catalog, addresses, cart, quote, orders, store orders, store catalog)
  - Ktor data sources and fakes
  - the payment flow moves into `core:payments` (DECISIONS 67)
  - the cart is kept in DataStore

## Acceptance (§13 Phase 3)
- The fee equals the §6 formula for 3 test distances (Pest on the server quote).
- Tampering with prices in the client has no effect: the order uses only the quote, and Pest sends forged prices.
- An online order stays PENDING_PAYMENT until the webhook; a failed payment cancels it (Pest + emulator with the
  fake checkout).
- A store not answering in 5 min auto-cancels the order, and refunds it if paid online (Pest with time travel +
  scheduler).
