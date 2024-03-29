CREATE SCHEMA write;

CREATE TABLE write.event_stream (
	id uuid NOT NULL,
	version smallint NOT NULL,
	payload jsonb NOT NULL,
	timeStamp timestamp NOT NULL,
	CONSTRAINT event_stream_pkey PRIMARY KEY(Id,Version)
);

CREATE TABLE write.event_stream_snapshots (
	id uuid NOT NULL,
	snapshot bytea NOT NULL,
	CONSTRAINT event_stream_snapshots_pkey PRIMARY KEY(Id)
);
