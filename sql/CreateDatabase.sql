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
    id          serial primary key,
    source_id   int  not null,
    type        int  not null,
    className   text not null,
    identifier  text,
    embed       text not null,
    source_link text,

    unique (source_id, className, identifier)
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

create index see_also_doc_id_index on docseealsoreference (doc_id);