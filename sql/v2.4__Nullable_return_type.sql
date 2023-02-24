update doxxy_version set version = '2.4' where one_row = true;

alter table doc alter column return_type drop not null;

update doc set return_type = null where type = 1;