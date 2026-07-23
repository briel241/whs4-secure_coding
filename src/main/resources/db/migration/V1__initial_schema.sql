CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    normalized_email VARCHAR(254) NOT NULL UNIQUE,
    nickname VARCHAR(30) NOT NULL,
    normalized_nickname VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BLOCKED', 'WITHDRAWN')),
    failed_login_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_users_status ON users(status);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    normalized_name VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    title VARCHAR(100) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    price BIGINT NOT NULL CHECK (price >= 0 AND price <= 100000000),
    item_condition VARCHAR(20) NOT NULL CHECK (item_condition IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR')),
    trade_method VARCHAR(20) NOT NULL CHECK (trade_method IN ('DIRECT', 'DELIVERY', 'BOTH')),
    trade_region VARCHAR(100),
    sale_status VARCHAR(20) NOT NULL CHECK (sale_status IN ('DRAFT', 'ON_SALE', 'RESERVED', 'SOLD', 'DELETED')),
    moderation_status VARCHAR(20) NOT NULL CHECK (moderation_status IN ('VISIBLE', 'UNDER_REVIEW', 'HIDDEN')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_products_public ON products(sale_status, moderation_status, created_at DESC);
CREATE INDEX idx_products_seller ON products(seller_id, created_at DESC);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_title_lower ON products(LOWER(title));

CREATE TABLE product_images (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    storage_key VARCHAR(100) NOT NULL UNIQUE,
    content_type VARCHAR(50) NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    byte_size BIGINT NOT NULL CHECK (byte_size > 0 AND byte_size <= 5242880),
    width INTEGER NOT NULL CHECK (width > 0 AND width <= 8000),
    height INTEGER NOT NULL CHECK (height > 0 AND height <= 8000),
    display_order INTEGER NOT NULL CHECK (display_order BETWEEN 0 AND 4),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(product_id, display_order)
);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    buyer_id UUID NOT NULL REFERENCES users(id),
    seller_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (buyer_id <> seller_id),
    UNIQUE(product_id, buyer_id, seller_id)
);

CREATE INDEX idx_conversations_buyer ON conversations(buyer_id, updated_at DESC);
CREATE INDEX idx_conversations_seller ON conversations(seller_id, updated_at DESC);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    body VARCHAR(1000) NOT NULL CHECK (char_length(body) BETWEEN 1 AND 1000),
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
CREATE INDEX idx_messages_sender_recent ON messages(sender_id, created_at DESC);

CREATE TABLE conversation_reads (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    user_id UUID NOT NULL REFERENCES users(id),
    last_read_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(conversation_id, user_id)
);

CREATE TABLE wallet_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE transfer_intents (
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    amount BIGINT NOT NULL CHECK (amount BETWEEN 1 AND 1000000),
    memo VARCHAR(200),
    idempotency_key UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'USED', 'EXPIRED', 'CANCELLED')),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (sender_id <> recipient_id)
);

CREATE INDEX idx_transfer_intents_sender ON transfer_intents(sender_id, status, expires_at);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    intent_id UUID NOT NULL UNIQUE REFERENCES transfer_intents(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    amount BIGINT NOT NULL CHECK (amount BETWEEN 1 AND 1000000),
    memo VARCHAR(200),
    idempotency_key UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('COMPLETED', 'REVERSED')),
    reference_transfer_id UUID REFERENCES transfers(id),
    completed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(sender_id, idempotency_key),
    CHECK (sender_id <> recipient_id)
);

CREATE INDEX idx_transfers_sender_date ON transfers(sender_id, completed_at DESC);
CREATE INDEX idx_transfers_recipient_date ON transfers(recipient_id, completed_at DESC);

CREATE TABLE wallet_ledger_entries (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallet_accounts(id),
    transfer_id UUID REFERENCES transfers(id),
    signed_amount BIGINT NOT NULL CHECK (signed_amount <> 0),
    balance_after BIGINT NOT NULL CHECK (balance_after >= 0),
    entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT', 'REVERSAL', 'GRANT')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE(wallet_id, transfer_id)
);

CREATE INDEX idx_ledger_wallet ON wallet_ledger_entries(wallet_id, created_at);

CREATE TABLE reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES users(id),
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('USER', 'PRODUCT', 'MESSAGE')),
    target_id UUID NOT NULL,
    report_type VARCHAR(30) NOT NULL CHECK (report_type IN ('FRAUD', 'PROHIBITED_ITEM', 'HARASSMENT', 'SPAM', 'PERSONAL_INFO', 'OTHER')),
    details VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    resolution_reason VARCHAR(1000),
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_reports_queue ON reports(status, created_at);
CREATE INDEX idx_reports_reporter_recent ON reports(reporter_id, created_at DESC);

CREATE TABLE moderation_actions (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES users(id),
    target_type VARCHAR(30) NOT NULL,
    target_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    before_state VARCHAR(100),
    after_state VARCHAR(100),
    reason VARCHAR(1000) NOT NULL,
    result VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_moderation_target ON moderation_actions(target_type, target_id, created_at DESC);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id UUID,
    result VARCHAR(30) NOT NULL,
    reason VARCHAR(1000),
    metadata VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id, created_at DESC);

INSERT INTO categories (id, name, normalized_name, active, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000101', '디지털 기기', '디지털 기기', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000102', '생활용품', '생활용품', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000103', '취미·도서', '취미·도서', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000104', '패션', '패션', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
