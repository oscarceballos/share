#!/bin/bash

#up docker
cd docker-compose
docker-compose up -d --scale taskmanager=$5
cd ..

# copy the .jar file to container
JOBMANAGER_CONTAINER=$(docker ps --filter name=jobmanager --format={{.ID}})
docker cp $2 "$JOBMANAGER_CONTAINER":$3/job.jar

# run the .jar file
docker exec -it "$JOBMANAGER_CONTAINER" flink run -p $6 $3/job.jar --dataset $4 --output $3/

# makes a request to REST API
JSON=`curl "http://localhost:8081/joboverview/completed"`

# get id of first job
JOB_ID=`echo ${JSON} | jq '.jobs[0].jid'`

# makes a request to REST API taking into account the job id
JSON_JOB=`curl http://localhost:8081/jobs/${JOB_ID//\"}`

# get durations from each subtask except the first and last
DURATIONS=`echo ${JSON_JOB} | jq '.vertices[].duration'`
DURATIONS_ARRAY=(`echo ${DURATIONS[*]}`)

# calculates the time of subtasks except the first and the last two
j=0
TIME=0
while [ $j -lt $[${#DURATIONS_ARRAY[*]} - 2] ]; do
    if [ $j -ne 0 ]; then
        let TIME+=${DURATIONS_ARRAY[$j]}
    fi
    let j+=1
done

# export total time into .txt file
DS=(${4//// })
echo "dataset:"${DS[${#DS[*]}-1]}" query:"$1" taskmanager:"$5" parallelism:"$6" time:"$TIME >> queries-time.txt

#down docker
cd docker-compose
docker-compose down -v
cd ..

exit
