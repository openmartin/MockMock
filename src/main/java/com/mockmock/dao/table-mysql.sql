-- auto-generated definition
create table mock_mail
(
  id               bigint        not null auto_increment
    primary key,
  mail_from        varchar(200)  null,
  mail_to          varchar(200)  null,
  mail_subject     varchar(200)  null,
  mail_raw         MEDIUMBLOB    NULL,
  receive_time     datetime      null,
  attatch_filename varchar(200)  null,
  attatchment      MEDIUMBLOB    NULL,
  mail_bcc         VARCHAR(1000) NULL
)
;

