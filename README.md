# What is AREX's storage service ?
Manage the recording and replaying data and repository it.
currently we use mongodb,
from the storage service we provide functions as follows:
## for the agent recording
1. provide a version number for all config files
1. save the entry point & all dependents 
## for the agent replaying
1. read the config file by version number before replaying
1. receive the request to diff and read a mock result as response
## for the schedule replaying
1. provide entry points as replay trigger data source
1. provide diff response source which saved from agent's replay
## others
1. a full mock data should be listed by the record id 
1. cache manage