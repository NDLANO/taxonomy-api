# taxonomy-api

[![Build Status](https://travis-ci.org/NDLANO/taxonomy-api.svg?branch=master)](https://travis-ci.org/NDLANO/taxonomy-api)

Rest service and graph database for organizing content

Unless otherwise specified, all PUT and POST requests must use 
`Content-Type: application/json;charset=UTF-8`. If charset is omitted, UTF-8 will be assumed. 

All GET requests will return data using the same content type. 

When you remove an entity, its associations are also deleted. E.g., if you remove a subject, 
its associations to any topics are removed. The topics themselves are not affected.

## `/subjects`

A collection of school subject, such as physics, mathematics or biology.

### GET `/subjects`
Gets a list of all subjects

*example input*
    
    GET /subjects
    
*example output*

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

### GET `/subjects/{id}`
Gets a single subject

*example input*

    GET /subjects/urn:subject:4288

*example output*

       {
          "id" : "urn:subject:4288",
          "name" : "biology"
       }

### GET `/subjects/{id}/topics`
Gets all topics directly associated with that subject. Note that this resource is read-only. 
To update the relationship between subjects and topics, use the resource `/subject-topics`.

*example input*

    GET /subjects/urn:subject:4288/topics

*example output*

    [
         {
            "id" : "urn:topic:4176",
            "name" : "photo synthesis"
         }
    ]

### PUT `/subjects/{id}`
Update a single subject

*example input*

    PUT /subjects/urn:subject:4288

    {
        "name" : "biology"
    }
    
*example output*

    < HTTP/1.1 204
    
### POST `/subjects`

Creates a new subject 

*example input*

        POST /subjects

        {
          "name" : "mathematics"
        }
       
*example output* 
       
       < HTTP/1.1 201
       < Location: /subjects/urn:subject:4208
       < Content-Length: 0


### DELETE `/subjects/{id}`
Removes a single subject 

*example input*

    DELETE /subjects/urn:subject:4208

*example output*

    < HTTP/1.1 204


## `/topics`

A collection of topics.  

### `GET /topics`
Gets a list of all topics

*example input* 

    GET /topics

*example output*

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

### GET `/topics/{id}`
Gets a single topic

*example input*

    GET /topics/urn:topic:4288

*example output*

       {
          "id" : "urn:topic:4288",
          "name" : "photo synthesis"
       }

### POST `/topics`

Creates a new topic 

*example input*

    POST /topics

    {
      "name" : "photo synthesis"
    }
       
*example output* 
       
    < HTTP/1.1 201
    < Location: /topics/urn:topic:4288
    < Content-Length: 0

## `/subject-topics`

Many-to-many association between subjects and topics. If you add a topic to a subject 
using this resource, the change will be also be visible at the read-only collection at 
`/subjects/{id}/topics`. 

### GET `/subject-topics`

Gets a list of all subjects and their first-level topics. A topic may have *one* primary subject, to help with
selecting a default context for a topic in case no context is given.

*example input* 

    GET /subject-topics

*example output* 

    [
       {
          "id" : "urn:subject-topic:odxco-3b4-27th-380",
          "topicid" : "urn:topic:4176",
          "subjectid" : "urn:subject:4208",
          "primary" : false
       },
       {
          "id" : "urn:subject-topic:1cruoe-38w-27th-1crx34",
          "primary" : false,
          "subjectid" : "urn:subject:4288",
          "topicid" : "urn:topic:4176"
       }
    ]

### POST `/subject-topics`

Associates a subject with a topic.  

*example input*

    POST /subject-topics

    {
      "topicid" : "urn:topic:4176",
      "subjectid" : "urn:subject:4208",
      "primary" : false
    }
    
*example output*
    
    < HTTP/1.1 201
    < Location: /subject-topics/urn:subject-topic:1cruoe-38w-27th-1crx34
    < Content-Length: 0
    
### PUT `/subject-topics/{id}`

Update the association between a subject and a topic. Changes to `subjectid` or `topicid` are not
allowed. Instead, remove the association and create a new one. 

*example input*

    PUT /subject-topics/urn:subject-topic:1cruoe-38w-27th-1crx34

    {
      "primary" : true
    }

*example output*

    < HTTP/1.1 204

### DELETE `/subject-topics/{id}`

Remove an association between a subject and a topic. 

*example input*

    DELETE /subject-topics/urn:subject-topic:1cruoe-38w-27th-1crx34

*example output* 
    
    < HTTP/1.1 204

