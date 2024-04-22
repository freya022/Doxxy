create table library_version
(
    group_id    text not null,
    artifact_id text not null,
    version     text not null,
    source_url  text null,

    primary key (group_id, artifact_id)
)