create table doc_mention
(
    message_id bigint not null,
    user_id    bigint not null,

    primary key (message_id, user_id)
);