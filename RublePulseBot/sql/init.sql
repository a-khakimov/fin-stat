CREATE DATABASE invest;
\c invest;

CREATE TABLE IF NOT EXISTS last_prices (
    instrumentUid text NOT NULL,
    figi text NOT NULL,
    tinvest_time timestamp,
    app_time timestamp NOT NULL,
    units bigint,
    nano integer
);
