# taxonomy-api

[![Build Status](https://travis-ci.org/NDLANO/taxonomy-api.svg?branch=master)](https://travis-ci.org/NDLANO/taxonomy-api)

Rest service and relational database for organizing content

## What does this service do?

This API is for organising content. Taxonomy covers subjects, topics, subtopics, resources, resource types, and how all these are connected. 
You can create, retrieve, update and delete (CRUD operations) both elements and the connections between elements.  

Below you can see a figure of how elements can be connected. Then we will go through how this structure can be realised through the API. 
![](mathematicsStructure.png?raw=true)

## Details

Unless otherwise specified, all PUT and POST requests must use
`Content-Type: application/json;charset=UTF-8`. If charset is omitted, UTF-8 will be assumed.
All GET requests will return data using the same content type.

When you remove an entity, its associations are also deleted. E.g., if you remove a subject,
its associations to any topics are removed. The topics themselves are not affected.

## `/subjects`

A collection of school subjects, such as physics, mathematics or biology.

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
Gets all topics associated with that subject. Note that this resource is read-only.
To update the relationship between subjects and topics, use the resource `/subject-topics`.

*parameters*

`recursive` `(true|false)` If true, subtopics are fetched recursively. Default: `false` 

*example input*

    GET /subjects/urn:subject:4288/topics?recursive=true

*example output*

    [
       {
          "name" : "photo synthesis",
          "id" : "urn:topic:4176",
          "subtopics" : []
       },
       {
          "name" : "trigonometry",
          "id" : "urn:topic:81924160",
          "subtopics" : [
             {
                "name" : "pythagoras",
                "id" : "urn:topic:4328",
                "subtopics" : []
             }
          ]
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

*properties*

`name` (`string`) - the name of the subject

`id` (`string`) - if specified, set the id to this value. Must start with `urn:subject:` and be a valid URI. 
If ommitted, an id will be assigned automatically. 

*example input*

        POST /subjects

        {
          "id" : "urn:subject:4208",
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

### GET `/topics/{id}/subtopics`
Gets a list of subtopics of a topic

*example input*

    GET /topics/urn:topic:4288/subtopics

*example output*

    [
       {
          "id" : "urn:topic:4285",
          "name" : "chlorophyll"
       },
       {
          "id" : "urn:topic:4222",
          "name" : "carbon cycle"
       }
    ]

### POST `/topics`

Creates a new topic

*properties*

`name` (`string`) - the name of the topic

`id` (`string`) - if specified, set the id to this value. Must start with `urn:topic:` and be a valid URI. 
If ommitted, an id will be assigned automatically. 

*example input*

    POST /topics

    {
      "id" : "urn:topic:4288",
      "name" : "photo synthesis"
    }

*example output*

    < HTTP/1.1 201
    < Location: /topics/urn:topic:4288
    < Content-Length: 0


### PUT `/topics/{id}`
Update a single topic

*example input*

    PUT /topics/urn:topic:4288

    {
        "name" : "trigonometry"
    }

*example output*

    < HTTP/1.1 204

### DELETE `/topics/{id}`
Removes a single topic

*example input*

    DELETE /topics/urn:topic:4288

*example output*

    < HTTP/1.1 204

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

## `/topic-subtopics`

Many-to-many association between topics and subtopics. If you add a subtopic to a topic
using this resource, the change will be also be visible at the read-only collection at
`/topics/{id}/subtopics`.

### GET `/topic-subtopics`

Gets a list of all topics and their subtopics. A subtopic may have *one* primary parent topic, to help with
selecting a default context for a subtopic in case no context is given.

*example input*

    GET /topic-subtopics

*example output*

    [
       {
          "id" : "urn:topic-subtopic:odxco-3b4-27th-380",
          "topicid" : "urn:topic:4176",
          "subtopicid" : "urn:topic:4208",
          "primary" : false
       },
       {
          "id" : "urn:topic-subtopic:1cruoe-38w-27th-1crx34",
          "primary" : false,
          "topicid" : "urn:topic:4176"
          "subtopicid" : "urn:topic:4288",
       }
    ]

### POST `/topic-subtopics`

Associates a topic with a subtopic

*example input*

    POST /topic-subtopics

    {
      "topicid" : "urn:topic:4176",
      "subtopicid" : "urn:topic:4208",
      "primary" : false
    }

*example output*

    < HTTP/1.1 201
    < Location: /topic-subtopics/urn:topic-subtopic:1cruoe-38w-27th-1crx34
    < Content-Length: 0

### PUT `/topic-subtopics/{id}`

Update the association between a topic and a subtopic. Changes to `topicid` or `subtopicid` are not
allowed. Instead, remove the association and create a new one.

*example input*

    PUT /topic-subtopics/urn:topic-subtopic:1cruoe-38w-27th-1crx34

    {
      "primary" : true
    }

*example output*

    < HTTP/1.1 204

### DELETE `/topic-subtopics/{id}`

Remove an association between a topic and a subtopic.

*example input*

    DELETE /topic-subtopics/urn:topic-subtopic:1cruoe-38w-27th-1crx34

*example output*

    < HTTP/1.1 204

## `/resources`

A collection of learning resources, such as articles, videos or learning paths.

### GET `/resources`
Gets a list of all resources

*example input*

    GET /resources

*example output*

    [
       {
          "id" : "urn:resource:4208",
          "name" : "The inner planets"
       },
       {
          "id" : "urn:resource:4288",
          "name" : "The gas giants"
       }
    ]

### GET `/resources/{id}`
Gets a single resource

*example input*

    GET /resources/urn:resource:4288

*example output*

       {
          "id" : "urn:resource:4288",
          "name" : "The inner planets"
       }

### PUT `/resources/{id}`
Update a single resource

*example input*

    PUT /resources/urn:resource:4288

    {
        "name" : "The rocky planets"
    }

*example output*

    < HTTP/1.1 204

### POST `/resources`

Creates a new resource

*properties*

`name` (`string`) - the name of the resource

`id` (`string`) - if specified, set the id to this value. Must start with `urn:resource:` and be a valid URI. 
If ommitted, an id will be assigned automatically. 

*example input*

        POST /resources

        {
          "id" : "urn:resource:4208",
          "name" : "The inner planets"
        }

*example output*

       < HTTP/1.1 201
       < Location: /resources/urn:resource:4208
       < Content-Length: 0

### DELETE `/resources/{id}`
Removes a single resource

*example input*

    DELETE /resources/urn:resource:4208

*example output*

    < HTTP/1.1 204

## `/topic-resources`

Many-to-many association between topics and resources. If you add a resource to a topic
using this resource, the change will be also be visible at the read-only collection at
`/topics/{id}/resources`.

### GET `/topic-resources`

Gets a list of all topics and their resources. A resource may have *one* primary parent topic, to help with
selecting a default context for a resource in case no context is given.

*example input*

    GET /topic-resources

*example output*

    [
       {
          "id" : "urn:topic-resource:odxco-3b4-27th-380",
          "topicid" : "urn:topic:4176",
          "resourceid" : "urn:resource:4208",
          "primary" : false
       },
       {
          "id" : "urn:topic-resource:1cruoe-38w-27th-1crx34",
          "primary" : false,
          "topicid" : "urn:topic:4176"
          "resourceid" : "urn:resource:4288",
       }
    ]

### POST `/topic-resources`

Associates a topic with a resource

*example input*

    POST /topic-resources

    {
      "topicid" : "urn:topic:4176",
      "resourceid" : "urn:resource:4208",
      "primary" : false
    }

*example output*

    < HTTP/1.1 201
    < Location: /topic-resources/urn:topic-resource:1cruoe-38w-27th-1crx34
    < Content-Length: 0

### PUT `/topic-resources/{id}`

Update the association between a topic and a resource. Changes to `topicid` or `resourceid` are not
allowed. Instead, remove the association and create a new one.

*example input*

    PUT /topic-resources/urn:topic-resource:1cruoe-38w-27th-1crx34

    {
      "primary" : true
    }

*example output*

    < HTTP/1.1 204

### DELETE `/topic-resources/{id}`

Remove an association between a topic and a resource.

*example input*

    DELETE /topic-resources/urn:topic-resource:1cruoe-38w-27th-1crx34

*example output*

    < HTTP/1.1 204
