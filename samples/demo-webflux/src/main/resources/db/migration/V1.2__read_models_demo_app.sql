CREATE TABLE read.list_of_purchases (
    aggregate_id uuid NOT NULL,
    version smallint NOT NULL,
	time_of_purchase timestamp NOT NULL,
	soda_brand text NOT NULL,
	CONSTRAINT list_of_purchases_pkey PRIMARY KEY(aggregate_id, version)
);
