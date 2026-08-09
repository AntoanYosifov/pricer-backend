create table spike (
    id bigint generated always as identity primary key,
    note text not null
);