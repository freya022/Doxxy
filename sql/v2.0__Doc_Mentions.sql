update doxxy_version set version = '2.0' where one_row = true;

create table doc_mention
(
    message_id bigint not null,
    user_id    bigint not null,

    primary key (message_id, user_id)
);
