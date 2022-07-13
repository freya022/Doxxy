create table Tag
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

create table Doc
(
    id        serial primary key,
    source_id int  not null,
    type      int  not null,
    parent_id int,
    name      text not null,
    embed     text not null
);

create table DocSeeAlsoReference
(
    id             serial primary key,
    doc_id         int references Doc on delete cascade,
    text           text not null,
    link           text not null,
    target_type    int  not null,
    full_signature text
);