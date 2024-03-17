CREATE TABLE series -- series is the table name.
(                           -- Must be unique within the folder.
    series_id    Int64,
    title        Text,
    series_info  Text,
    release_date Int64,
    PRIMARY KEY (series_id) -- The primary key is a column or
    -- combination of columns that uniquely identifies
    -- each table row (contains only
    -- non-repeating values). A table can have
    -- only one primary key. For every table
    -- in YDB, the primary key is required.
);

CREATE TABLE seasons
(
    series_id   Int64,
    season_id   Int64,
    title       Text,
    first_aired Int64,
    last_aired  Int64,
    PRIMARY KEY (series_id, season_id)
);

CREATE TABLE episodes
(
    series_id  Int64,
    season_id  Int64,
    episode_id Int64,
    title      Text,
    air_date   Int64,
    PRIMARY KEY (series_id, season_id, episode_id)
);

COMMIT;