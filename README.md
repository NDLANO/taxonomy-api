# taxonomy-api

[![Build Status](https://travis-ci.org/NDLANO/taxonomy-api.svg?branch=master)](https://travis-ci.org/NDLANO/taxonomy-api)

Rest service and relational database for organizing content

## What does this service do?

This API is for organising content. There are two main types of content, *elements* and *connections* between elements. 
The elements in the taxonomy are subjects, topics, resources, and resource types. The taxonomy stores metadata for each element. 
This can be name (i.e. Mathematics) and URI to the content page. All elements, except resource types, can have content URIs. 

In addition to the elements, the taxonomy stores the connections you make between elements. Each connection also has 
metadata attached to it, such as which elements are connected, and whether or not this connection is the primary connection.
Subjects can be connected to topics, topics to subtopics, topics to resources, resources to resource types, and resource types to sub resources types. 

Below you can see a figure of how elements can be connected. We will go through how this structure can be realised 
through the API. For details on the use of each service, please see the Swagger documentation. 

![Figure of content structure for mathematics](mathematicsStructure.png?raw=true)

### Subjects and topics

First, create a Subject with the name Mathematics with a POST call to `/subjects`. When this call returns you'll get a location.
This location contains the URI for the subject, i.e. `urn:subject:342`. Any time you need to change or retrieve this subject 
you'll be using this id. 

Next create two Topic elements for Geometry and Statistics (POST to `topics`). If you have content for the elements, 
you can include the URI. The URIs can also be added  later (PUT to `/subjects` or `/topics`).

Once you have a subject and a topic, you can connect them. Use the ids for the two elements you want to 
connect. Connect a subject and a topic with a POST call to `/subject-topics`.
The first connection between a subject and a topic will automatically be marked as the primary connection. Elements (except 
subjects) can have multiple parent connections (see below for details).

A topic can have subtopics. In our example Trigonometry is a subtopic of Geometry. To connect the two, create a topic named Trigonometry. 
Then add a connection between the Geometry topic and the Trigonometry topic with a POST call to `/topic-subtopics`. 

The figure above also contains a topic for Statistics and the subtopic Probability. These can be connected in the same 
manner as previously described. The subject Mathematics will then have two topics, while each topic will have a subtopic.
Call GET on `/subjects/{id}/topics` to list out the topics connected to the subject.  

A GET call to `topics` will yield both topics and subtopics. The only thing differentiating a topic 
from a subtopic is the connection in `/topic-subtopic`. Similar to the connections between a subject and its topics, you can 
get all subtopics for a topic with a GET call to `/topics/{id}/subtopics`. 


### Updating elements and connections

All PUT calls will overwrite the information in the element. Be sure to include everything you want to keep. The taxonomy 
API does not check for empty fields unless they are required. The easiest way to update an element is to first retrieve 
the current element with a GET call to the correct service and then return the object with a PUT call after you make your changes. 


### Resources 

Resources are created in the same way as topics and subjects, but with a POST call to `/resources`. Resources can only 
be connected to topics. This happens with POST calls to `/topic-resources`. A resource can only be connected to a subject 
via its topic(s). Resources can have content URIs in the same way as topics and subjects. If you want to change the 
resource by adding a content uri, you use a PUT call to `/resources/{id}`. 

List all resources connected to a subject with a GET call to `/subjects/{id}/resources`. For the
Mathematics subject, this call would return a list with these five elements: Tangens, Sine and Cosine, What is probability, 
Adding probabilities, and Probability questions. 

You can also list all resources connected to a topic with a GET call to `/topics/{id}/resources`. If you want to list all 
resources for the topic Probability, you'll get back a list with three resources; What is probability, Adding probabilities 
and Probability questions. 

If you get all resources connected to the topic Statistics, you'll get an empty list, because it doesn't have any 
resources connected directly to it. If you ask for all resources recursively (`/topics/{id}/resources?recursive=true`), 
you'll get the three resources from the Probability topic. 


### Resource types

Resources can be tagged with resource types. First, create the resource type with a POST call to `/resource-types`. Then 
connect the resource to the resource type with a POST call to `/resource-resourcetypes` including both the uri of the 
resource and the resource type. A resource can have multiple resource types. 

When you get all resources for a subject or topic you can choose to get only resources matching a particular resource type 
(or a list of resource types).For our example, a GET call to `/subjects/{id}/resources?type={id}` with the ID for 
articles will give you a list of three elements; Sine and Cosine, What is probability, and Adding probability.



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
