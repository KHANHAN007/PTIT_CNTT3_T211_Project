create table users (
    id bigint primary key auto_increment,
    username varchar(50) not null unique,
    password varchar(255) not null,
    email varchar(255) not null unique,
    full_name varchar(255) not null,
    phone varchar(50) not null unique,
    role varchar(30) not null,
    enabled bit not null,
    kyc bit not null,
    pin varchar(255),
    failed_login_attempts int not null default 0,
    locked_until datetime(6),
    created_at datetime(6) not null
);

create table accounts (
    id bigint primary key auto_increment,
    account_number varchar(50) not null unique,
    owner_id bigint not null,
    balance decimal(19,2) not null,
    currency varchar(3) not null,
    active bit not null,
    version bigint,
    created_at datetime(6) not null,
    constraint fk_accounts_owner foreign key (owner_id) references users(id)
);

create table bank_transactions (
    id bigint primary key auto_increment,
    reference varchar(100) not null unique,
    from_account_id bigint null,
    to_account_id bigint null,
    amount decimal(19,2) not null,
    type varchar(30) not null,
    status varchar(30) not null,
    description varchar(255),
    idempotency_key varchar(255),
    external_bank_code varchar(100),
    external_account_number varchar(100),
    risk_score int,
    failure_reason varchar(255),
    created_at datetime(6) not null,
    constraint fk_tx_from foreign key (from_account_id) references accounts(id),
    constraint fk_tx_to foreign key (to_account_id) references accounts(id)
);

create table ledger_entries (
    id bigint primary key auto_increment,
    transaction_id bigint not null,
    account_id bigint not null,
    direction varchar(20) not null,
    amount decimal(19,2) not null,
    balance_after decimal(19,2) not null,
    created_at datetime(6) not null,
    constraint fk_ledger_tx foreign key (transaction_id) references bank_transactions(id),
    constraint fk_ledger_account foreign key (account_id) references accounts(id)
);

create table kyc_profiles (
    id bigint primary key auto_increment,
    user_id bigint not null unique,
    document_url varchar(1000) not null,
    id_number_encrypted longtext,
    status varchar(30) not null,
    rejection_reason varchar(500),
    verified_at datetime(6),
    updated_at datetime(6) not null,
    constraint fk_kyc_user foreign key (user_id) references users(id)
);

create table kyc_reviews (
    id bigint primary key auto_increment,
    profile_id bigint not null,
    reviewer_id bigint not null,
    decision varchar(30) not null,
    reason varchar(500),
    created_at datetime(6) not null,
    constraint fk_review_profile foreign key (profile_id) references kyc_profiles(id),
    constraint fk_review_user foreign key (reviewer_id) references users(id)
);

create table refresh_token (
    id bigint primary key auto_increment,
    token_hash varchar(64) not null unique,
    family_id varchar(255) not null,
    user_id bigint not null,
    expires_at datetime(6) not null,
    revoked bit not null,
    created_at datetime(6) not null,
    constraint fk_refresh_user foreign key (user_id) references users(id)
);

create table transfer_approvals (
    id bigint primary key auto_increment,
    transaction_id bigint not null unique,
    checker_id bigint,
    decision varchar(30),
    reason varchar(500),
    decided_at datetime(6),
    created_at datetime(6) not null,
    constraint fk_approval_tx foreign key (transaction_id) references bank_transactions(id),
    constraint fk_approval_checker foreign key (checker_id) references users(id)
);

create table audit_logs (
    id bigint primary key auto_increment,
    actor varchar(255),
    action varchar(255) not null,
    outcome varchar(50) not null,
    resource_id varchar(255),
    ip_address varchar(100),
    correlation_id varchar(255),
    details varchar(1000),
    created_at datetime(6) not null
);

create index idx_tx_statement_from on bank_transactions(from_account_id, created_at);
create index idx_tx_statement_to on bank_transactions(to_account_id, created_at);
create index idx_tx_status on bank_transactions(status);
create unique index uk_tx_idempotency_owner on bank_transactions(idempotency_key, from_account_id);
create index idx_audit_created_at on audit_logs(created_at);
