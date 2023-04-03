update doxxy_version
set version = '2.4' --TODO change
where one_row = true;

drop table if exists class, subclass, method, implementation cascade;

-- Table containing any class
create table class
(
    id             serial not null primary key,
    source_id      int    not null,
    qualified_name text   not null unique,
    source_link    text   not null
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