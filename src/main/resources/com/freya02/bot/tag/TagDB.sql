create table if not exists Tag
(
    guildId bigint not null,
    ownerId bigint not null,
    name    text   not null,
    text    text   not null,
    uses    int    not null default 0 check (uses >= 0),

    primary key (guildId, name)
);

select name from Tag where guildid = 722891685755093072 order by name offset 0 limit 10;
select name from Tag where guildid = 722891685755093072 order by uses desc offset 0 limit 10;