create table example
(
    id    serial not null,
    title text   not null,

    primary key (id),
    unique (title)
);

create table example_content
(
    id         serial not null,
    example_id int    not null,
    language   text   not null,
    content    text   not null,

    primary key (id),
    foreign key (example_id) references example (id) on delete cascade,
    unique (example_id, language)
);

create table example_target
(
    id         serial not null,
    example_id int    not null,
    target     text   not null,

    primary key (id),
    foreign key (example_id) references example (id) on delete cascade,
    unique (example_id, target),
    -- Match:
    -- ClassName
    -- ClassName#FIELD
    -- ClassName#Method
    -- But not:
    -- ClassName#Method()
    -- ClassName#Method(Collection<? super OptionData>)
    check (regexp_like(target, '^[A-Z][A-Za-z]+(?:#(?:[[:upper:]]+$|\w+)$)?$'))
);