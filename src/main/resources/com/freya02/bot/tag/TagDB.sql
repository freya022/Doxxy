create table if not exists Tag
(
    guildId     bigint    not null,
    ownerId     bigint    not null,
    createdAt   timestamp not null default now(),
    name        text      not null,
    description text      not null,
    content     text      not null,
    uses        int       not null default 0 check (uses >= 0),

    primary key (guildId, name)
);

alter table Tag drop constraint if exists name;
alter table Tag drop constraint if exists description;
alter table Tag drop constraint if exists content;