#!/bin/bash -x

CSM_DB=csm_for_secure_widgets

dropdb $CSM_DB
createdb $CSM_DB
psql $CSM_DB -f sql/AuthSchemaPostgres.sql
psql $CSM_DB -f sql/DataPrimingPostgres.sql
