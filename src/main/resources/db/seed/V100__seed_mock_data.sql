-- Dev-only seed data. Password for every seeded customer: password
INSERT INTO customers (id, email, full_name, password_hash, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'thandi@example.com', 'Thandi Mokoena', '$2a$10$uH4iYW5ItOVpiBWvK1B1vudUSIe6zbltb6ZYPw691vHHYXNqNyCTe', '2026-08-01 08:00:00+02'),
    ('22222222-2222-2222-2222-222222222222', 'ben@example.com', 'Ben Adams', '$2a$10$uH4iYW5ItOVpiBWvK1B1vudUSIe6zbltb6ZYPw691vHHYXNqNyCTe', '2026-08-01 08:00:00+02');

INSERT INTO cards (id, customer_id, last_four, created_at) VALUES
    ('a0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '4242', '2026-08-01 08:05:00+02'),
    ('a0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', '9001', '2026-08-01 08:06:00+02'),
    ('a0000000-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', '5555', '2026-08-01 08:07:00+02');

-- Amounts in cents (minor units).
INSERT INTO transactions (id, card_id, merchant_name, amount_minor, currency, occurred_at, created_at) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Woolworths Sandton',   45990, 'ZAR', '2026-09-02 18:20:00+02', '2026-09-02 18:20:05+02'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Takealot.com',       129900, 'ZAR', '2026-09-05 09:12:00+02', '2026-09-05 09:12:05+02'),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Takealot.com',       129900, 'ZAR', '2026-09-05 09:13:00+02', '2026-09-05 09:13:05+02'),
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Netflix',             19900, 'ZAR', '2026-09-10 00:01:00+02', '2026-09-10 00:01:05+02'),
    ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002', 'Uber Eats',           28750, 'ZAR', '2026-09-12 21:44:00+02', '2026-09-12 21:44:05+02'),
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000003', 'Airbnb Cape Town',   765000, 'ZAR', '2026-08-20 14:00:00+02', '2026-08-20 14:00:05+02');

INSERT INTO disputes (id, transaction_id, customer_id, reason, description, disputed_amount_minor, status, version, created_at, updated_at) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'DUPLICATE_CHARGE', 'Charged twice for the same order', 129900, 'SUBMITTED', 0, '2026-09-06 08:00:00+02', '2026-09-06 08:00:00+02'),
    ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'INCORRECT_AMOUNT', 'Charged R287.50, expected R250.00', 3750, 'UNDER_REVIEW', 1, '2026-09-13 10:00:00+02', '2026-09-14 09:30:00+02'),
    ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000006', '22222222-2222-2222-2222-222222222222', 'SERVICE_NOT_PROVIDED', 'Host cancelled the booking, no refund issued', 765000, 'RESOLVED_REJECTED', 2, '2026-08-25 11:00:00+02', '2026-09-03 16:00:00+02'),
    ('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'UNAUTHORIZED_TRANSACTION', 'Did not sign up for this plan', 19900, 'WITHDRAWN', 1, '2026-09-11 07:30:00+02', '2026-09-11 12:00:00+02');

INSERT INTO dispute_status_history (dispute_id, from_status, to_status, changed_by, note, changed_at) VALUES
    ('d0000000-0000-0000-0000-000000000001', NULL,        'SUBMITTED',         'customer',       NULL,                                      '2026-09-06 08:00:00+02'),
    ('d0000000-0000-0000-0000-000000000002', NULL,        'SUBMITTED',         'customer',       NULL,                                      '2026-09-13 10:00:00+02'),
    ('d0000000-0000-0000-0000-000000000002', 'SUBMITTED', 'UNDER_REVIEW',      'agent:ops-01',   'Merchant statement requested',            '2026-09-14 09:30:00+02'),
    ('d0000000-0000-0000-0000-000000000003', NULL,        'SUBMITTED',         'customer',       NULL,                                      '2026-08-25 11:00:00+02'),
    ('d0000000-0000-0000-0000-000000000003', 'SUBMITTED', 'UNDER_REVIEW',      'agent:ops-01',   NULL,                                      '2026-08-27 10:15:00+02'),
    ('d0000000-0000-0000-0000-000000000003', 'UNDER_REVIEW', 'RESOLVED_REJECTED', 'agent:ops-02', 'Host confirmed the booking was cancelled', '2026-09-03 16:00:00+02'),
    ('d0000000-0000-0000-0000-000000000004', NULL,        'SUBMITTED',         'customer',       NULL,                                      '2026-09-11 07:30:00+02'),
    ('d0000000-0000-0000-0000-000000000004', 'SUBMITTED', 'WITHDRAWN',         'customer',       'Customer withdrew the dispute',           '2026-09-11 12:00:00+02');
