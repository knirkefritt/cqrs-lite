CREATE SCHEMA write;
CREATE SCHEMA read;

CREATE TABLE write.event_stream (
	id uuid NOT NULL,
	version smallint NOT NULL,
	payload jsonb NOT NULL,
	timestamp timestamp NOT NULL,
	CONSTRAINT event_stream_pkey PRIMARY KEY(Id,Version)
);

CREATE TABLE write.event_stream_snapshots (
	id uuid NOT NULL,
	snapshot bytea NOT NULL,
	CONSTRAINT event_stream_snapshots_pkey PRIMARY KEY(Id)
);

CREATE TABLE write.outbox (
    id bigint GENERATED ALWAYS AS IDENTITY,
    key varchar NOT NULL,
    payload varchar NOT NULL,
    CONSTRAINT outbox_pkey PRIMARY KEY(Id)
);

CREATE TABLE write.external_id_lookup (
	external_id varchar NOT NULL,
	aggregate_type varchar NOT NULL,
	aggregate_id uuid NOT NULL,
	CONSTRAINT external_id_lookup_aggregate_pkey PRIMARY KEY(external_id, aggregate_type)
);