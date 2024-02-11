drop materialized view if exists doc_view cascade;
drop table if exists tag, doc, docseealsoreference, class, subclass, method, implementation;

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
    return_type            text,
    embed                  text not null,
    javadoc_link           text,                                               --Null for offline docs
    source_link            text,

    unique (source_id, className, identifier)
);

-- raspbian doesn't support postgresql 12+ lmao
-- create index doc_identifier_no_args_gist on doc using gist(identifier_no_args gist_trgm_ops(siglen=256));
create index doc_classname_gist on doc using gist (classname gist_trgm_ops);
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

create materialized view doc_view as
select id,
       case when identifier is null then classname else concat(classname, '#', identifier) end as full_identifier
from doc;

-- Doesn't need to be temporarily disabled since the materialized view is only refreshed once
create index doc_view_full_identifier_gist on doc_view using gist(full_identifier gist_trgm_ops);

--------------- Implementation metadata ---------------

-- Table containing any class
create table class
(
    id           serial not null primary key,
    source_id    int    not null,
    class_type   int    not null,
    package_name text   not null,
    class_name   text   not null,
    source_link  text   not null
);

-- Subclass relations
create table subclass
(
    superclass_id int not null references class on delete cascade,
    subclass_id   int not null references class on delete cascade,

    primary key (superclass_id, subclass_id)
);

-- **Declared** methods, i.e. does not contain inherited methods
create table method
(
    id          serial not null primary key,
    class_id    int    not null references class on delete cascade,
    method_type int    not null,
    name        text   not null,
    signature   text   not null,
    source_link text   not null
);

-- Link between class class id (for example the ID of VoiceChannelManager),
--  a (potentially inherited) method ID, to it's implementation (in a superclass)
-- Typical usage would be getting a method implementation such as:
--      VoiceChannelManager#removePermissionOverride
--      Get `implementation` by VoiceChannelManager's ID (join with `class`)
--      Then get the implementation data by joining `method` with `implementation_id`
--      and finally the implementation owner (`class`) with `class_id`
create table implementation
(
    class_id          int not null references class on delete cascade,
    method_id         int not null references method on delete cascade,
    implementation_id int not null references method on delete cascade,

    primary key (class_id, method_id, implementation_id)
);