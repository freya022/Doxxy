CREATE TABLE library_version
(
    id          SERIAL NOT NULL,
    group_id    TEXT   NOT NULL,
    artifact_id TEXT   NOT NULL,
    classifier  TEXT   NULL,
    version     TEXT   NOT NULL,
    source_url  TEXT   NULL,

    PRIMARY KEY (id),
    CONSTRAINT library_coordinates_key UNIQUE NULLS NOT DISTINCT (group_id, artifact_id, classifier)
)