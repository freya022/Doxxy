update doxxy_version
set version = '2.4' --TODO change
where one_row = true;

drop table if exists class, subclass, method, implementation cascade;

-- Table containing any class
create table class
(
    id           serial not null primary key,
    source_id    int    not null,
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

create or replace function get_subclasses(_class_name text)
    returns table
            (
                superclass_class_name text,
                subclass_class_name   text
            )
    language sql
as
'select c.class_name,
        subclass.class_name
 from class c
          join subclass sub on sub.superclass_id = c.id
          join class subclass on subclass.id = sub.subclass_id
 where c.class_name = _class_name';

create or replace function get_superclasses(_class_name text)
    returns table
            (
                subclass_class_name   text,
                superclass_class_name text
            )
    language sql
as
'select c.class_name,
        superclass.class_name
 from class c
          join subclass sub on sub.subclass_id = c.id
          join class superclass on superclass.id = sub.superclass_id
 where c.class_name = _class_name';

create or replace function get_implementations(_class_name text, _method_name text)
    returns table
            (
                class_name text,
                signature      text,
                source_link    text
            )
    language sql
as
'select implementation_owner.class_name,
        implementation.signature,
        implementation.source_link
 from implementation impl
          join class superclass on impl.class_id = superclass.id
          join method implementation
               on impl.implementation_id = implementation.id
          join class implementation_owner
               on implementation.class_id = implementation_owner.id
 where superclass.class_name = _class_name
   and implementation.name = _method_name';