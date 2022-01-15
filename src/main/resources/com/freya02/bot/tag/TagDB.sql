create table if not exists Tag
(
    guildId     bigint not null,
    ownerId     bigint not null,
    name        text   not null check (length(name) <= 100),
    description text   not null,
    text        text   not null,
    uses        int    not null default 0 check (uses >= 0),

    primary key (guildId, name)
);