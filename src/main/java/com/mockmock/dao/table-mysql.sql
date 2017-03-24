-- auto-generated definition
create table mock_mail
(
  id bigint not null auto_increment
    primary key,
  mail_from varchar(200) null,
  mail_to varchar(200) null,
  mail_subject varchar(200) null,
  mail_raw blob null,
  receive_time datetime null,
  attatch_filename varchar(200) null,
  attatchment blob null
)
;

