# taxonomy-api

[![Build Status](https://travis-ci.org/NDLANO/taxonomy-api.svg?branch=master)](https://travis-ci.org/NDLANO/taxonomy-api)

Rest service and graph database for organizing content

Unless otherwise specified, all PUT and POST requests must use 
`Content-Type: application/json;charset=UTF-8`. If charset is omitted, UTF-8 will be assumed. 

All GET requests will return data using the same content type. 

## /subjects

A collection of school subject, such as physics, mathematics or biology.

### GET /subjects
Gets a list of all subjects

*output*

    [
       {
          "id" : "urn:subject:4208",
          "name" : "mathematics"
       },
       {
          "id" : "urn:subject:4288",
          "name" : "biology"
       }
    ]

### GET /subjects/{id}
Gets a single subject

*input*

    GET /subjects/urn:subject:4288

*output*

       {
          "id" : "urn:subject:4288",
          "name" : "biology"
       }

### GET /subjects/{id}/topics
Gets all topics directly associated with that subject. Note that this resource is read-only. 
To update the relationship between subjects and topics, use the resource `/subject-topics`.

*input*

    GET /subjects/urn:subject:4288/topics

*output*

    [
         {
            "id" : "urn:topic:4176",
            "name" : "photo synthesis"
         }
    ]

    
### POST /subjects

Creates a new subject 

*input*

        {
          "name" : "mathematics"
        }
       
*output* 
       
       < HTTP/1.1 201
       < Location: /subjects/urn:subject:4208
       < Content-Length: 0




## /topics

A collection of topics.  

### GET /topics
Gets a list of all topics

*output*

    [
       {
          "id" : "urn:topic:4208",
          "name" : "trigonometry"
       },
       {
          "id" : "urn:topic:4288",
          "name" : "photo synthesis"
       }
    ]

### GET /topics/{id}
Gets a single topic

*input*

    GET /topics/urn:topic:4288

*output*

       {
          "id" : "urn:topic:4288",
          "name" : "photo synthesis"
       }

### POST /topics

Creates a new topic 

*input*

        {
          "name" : "photo synthesis"
        }
       
*output* 
       
       < HTTP/1.1 201
       < Location: /topics/urn:topic:4288
       < Content-Length: 0


## /subject-topics

Many-to-many association between subjects and topics. If you add a topic to a subject 
using this resource, the change will be also be visible at the read only collection at 
`/subjects/{id}/topics`. 

    [
       {
          "topicid" : "urn:topic:4176",
          "subjectid" : "urn:subject:4208",
          "primary" : false
       },
       {
          "primary" : false,
          "subjectid" : "urn:subject:4288",
          "topicid" : "urn:topic:4176"
       }
    ]
