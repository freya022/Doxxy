update doxxy_version
set version = '2.4' --TODO change
where one_row = true;

drop table if exists doc_link, doc_doc_link cascade;

-- Table containing the links to a definition
create table doc_link
(
    id          serial not null primary key,
    source_id   int    not null,
    type        int    not null,
    def         text   not null unique, --Either class or method signature (MyClass#method(ParamType1, ParamType2))
    source_link text   not null
);

-- Table linking superclass -> subclasses or method definition -> method declaration
create table doc_doc_link
(
    super_def  text not null,
    sub_def_id int  not null references doc_link on delete cascade,

    primary key (super_def, sub_def_id)
);