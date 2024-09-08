CREATE DATABASE invest;
\c invest;

CREATE TABLE IF NOT EXISTS last_prices (
    instrumentUid TEXT NOT NULL,
    figi TEXT NOT NULL,
    tinvest_time TIMESTAMP,
    app_time TIMESTAMP NOT NULL,
    price NUMERIC(12, 6),
    processed BOOLEAN NOT NULL
);
