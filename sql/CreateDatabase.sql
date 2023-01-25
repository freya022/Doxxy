drop table if exists tag, doc, docseealsoreference, doc_mention;

create extension if not exists pg_trgm;

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
    id                     serial primary key,
    source_id              int  not null,
    type                   int  not null,
    className              text not null,
    identifier             text,                                               --For choice value, "method(Type1, Type2)"
    identifier_no_args     text check (length(identifier_no_args) <= 100),     --For search purposes, "method"
    human_identifier       text check (length(human_identifier) <= 100),       --For class-specific choice name, "method(Type name, name2)"
    human_class_identifier text check (length(human_class_identifier) <= 100), --For any-class choice name, "Class#method(Type name, name2)"
    return_type            text not null,
    embed                  text not null,
    javadoc_link           text,                                               --Null for offline docs
    source_link            text,

    unique (source_id, className, identifier)
);

-- raspbian doesn't support postgresql 12+ lmao
-- create index doc_identifier_no_args_gist on doc using gist(identifier_no_args gist_trgm_ops(siglen=256));
create index doc_identifier_no_args_gist on doc using gist (identifier_no_args gist_trgm_ops);

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

create table doc_mention
(
    message_id bigint not null,
    user_id    bigint not null,

    primary key (message_id, user_id)
);