#!/bin/bash

# copy the .jar file to container
docker cp $2 $(docker ps --filter name=jobmanager --format={{.ID}}):$3

# run the .jar file
docker exec -it $(docker ps --filter name=jobmanager --format={{.ID}}) flink run -m jobmanager:6123 -c org.univalle.rdf.out.Query /data/query-1.0-SNAPSHOT.jar --dataset $4 --output $3/

# makes a request to REST API
JSON=`curl "http://localhost:8081/joboverview/completed"`

# get the job id
JID1=`echo ${JSON} | jq '.jobs[].jid'`

JID2="${JID1//\"}"

# makes a request to REST API taking into account the job id
JSON_JID=`curl http://localhost:8081/jobs/${JID2}`

# get durations from each subtask except the first and last
i=0
DURATION=0
for d in `echo ${JSON_JID} | jq '.vertices[].duration'`; do
        let DURATION[i]=d
        let i+=1
done

# calculates the time of subtasks
j=0
TIME=0
while [ $j -lt $[${#DURATION[*]} - 1] ]; do
    if [ $j -ne 0 ]; then
        let TIME+=${DURATION[$j]}
    fi
    let j+=1
done

# export total time into txt file
echo "Query" $1 "time: " $TIME >> query-time.txt

exit
