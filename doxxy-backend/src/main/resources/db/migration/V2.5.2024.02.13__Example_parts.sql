create table example_content_part
(
    id                 serial not null,
    example_content_id int    not null,
    label              text   not null,
    emoji              text   null,
    description        text   null,
    content            text   not null,

    primary key (id),
    foreign key (example_content_id) references example_content (id) on delete cascade,
    constraint label_length check (length(label) < 100),
    constraint description_length check (length(description) < 100),
    constraint content_length check (length(content) < 2000)
);

insert into example_content_part (example_content_id, label, content)
select c.id, e.title, c.content
from example_content c
         join example e on e.id = c.example_id;

alter table example_content
    drop column content;