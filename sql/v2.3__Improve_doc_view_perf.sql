update doxxy_version set version = '2.3' where one_row = true;

drop materialized view doc_view;

create materialized view doc_view as
select id,
       case when identifier is null then classname else concat(classname, '#', identifier) end as full_identifier
from doc;

-- Doesn't need to be temporarily disabled since the materialized view is only refreshed once
create index doc_view_full_identifier_gist on doc_view using gist(full_identifier gist_trgm_ops);