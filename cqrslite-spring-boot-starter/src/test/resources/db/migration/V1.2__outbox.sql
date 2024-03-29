CREATE TABLE write.outbox (
    id bigint GENERATED ALWAYS AS IDENTITY,
    key varchar NOT NULL,
    payload varchar NOT NULL,
    CONSTRAINT outbox_pkey PRIMARY KEY(Id)
);