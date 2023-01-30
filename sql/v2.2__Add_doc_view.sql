update doxxy_version set version = '2.2' where one_row = true;

create materialized view doc_view as
select id, concat(classname, '#', identifier) as full_identifier
from doc
where identifier is not null;

--TODO refresh trigger
