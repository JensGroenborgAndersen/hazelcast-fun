#!/bin/bash

docker run -e JAVA_MAX_HEAP_SIZE=1g -e TEST_DB_URL="db_user:db_password@locahost:5432/db_database" -p 8080:8080 docker-metascrum.artifacts.dbccloud.dk/hazelcast-fun:devel
