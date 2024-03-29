CREATE TABLE write.external_id_lookup (
	external_id varchar NOT NULL,
	aggregate_type varchar NOT NULL,
	aggregate_id uuid NOT NULL,
	CONSTRAINT external_id_lookup_aggregate_pkey PRIMARY KEY(external_id, aggregate_type)
);