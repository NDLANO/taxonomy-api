# taxonomy-api

[![Build Status](https://travis-ci.org/NDLANO/taxonomy-api.svg?branch=master)](https://travis-ci.org/NDLANO/taxonomy-api)

Rest service and relational database for organizing content

## What does this service do?

This API is for organising and categorising the content provided by NDLA. At the heart of the structure are the Resources, which
represent the actual content, such as articles, videos of lectures, quizzes, learning paths (which are compositions of other 
resources) or any number of other media. The organisation of content allows for a context rich user interface to be built on top
of this API where the content is displayed in context of a Subject and its Topics, with hyperlinks to related content. 

Please note that this API is all about metadata. The actual content is stored in other APIs, identified by the Content URI
stored with each Subject, Topic and Resource in this API. 

The Resources are categorised according to Subjects and Topics. A Subject is a high-level field of study, such as Mathematics
or Science, loosely based on the subject curricula at [udir.no](https://www.udir.no/kl06/MAT1-04?lplang=eng), whereas a Topic 
is a hierarchical sub-division of the subject into subject areas. The topics may be loosely based on the subject areas at 
[udir.no](https://www.udir.no/kl06/MAT1-04/Hele/Hovedomraader?lplang=eng), or they may follow a different structure imposed by the
editors.

This organisation gives us a tree representation of the content, where the Subjects are at the roots of the tree, the 
Topics make up the branches, and the Resources are the leaves. Note, however, that this is not a strict tree-structure, since
topics and resources may belong to several parents (see "Multiple parent connections" below). 

The taxonomy data model consists of *entities* and *connections* between entities. 

The entities in the taxonomy are Subjects, Topics, Resources, and Resource Types. The taxonomy stores metadata for each entity, 
like Name and Content URI. 

In addition to the entities, the taxonomy stores the connections you make between entities. Each connection also has 
metadata attached to it, such as which entities are connected, and whether or not this connection is the primary connection 
(see "Multiple parent connections" below).
Subjects can be connected to topics, topics to subtopics, topics to resources, resources to resource types, and resource types 
to parent resources types. 

Below you can see a figure of how entities can be connected. We will go through how this structure can be realised 
through the API. For details on the use of each service, please see the Swagger documentation. 

![Figure of content structure for mathematics](doc/mathematics-structure.png?raw=true)

### Subjects and topics

First, create a Subject with the name Mathematics with a POST call to `/subjects`. When this call returns you'll get a location.
This location contains the path to this subject within the subjects resource, e.g., `/v1/subjects/urn:subject:342`, where `urn:subject:342` 
is the ID of the newly created subject. Any time you need to change or retrieve this subject you'll be using this ID. 

Next, create two Topic entities for Geometry and Statistics (POST to `topics`). If you have a topic description that you want to use
as the content for these entities, you can include the content URI. The content URIs can also be added later (PUT to `/subjects` or `/topics`).

Once you have a subject and a topic, you can connect them. Connect a subject and a topic with a POST call to `/subject-topics`. 
Use the IDs for the two entities you want to connect. Topics and resources can have multiple parent parent connections.
The first connection between a subject and a topic will automatically be marked as the primary connection (see below for details).

A topic can have subtopics. In our example Trigonometry is a subtopic of Geometry. To connect the two, create a topic named Trigonometry. 
Then add a connection between the Geometry topic and the Trigonometry topic with a POST call to `/topic-subtopics`. 

The figure above also contains a topic for Statistics and the subtopic Probability. These can be connected in the same 
manner as previously described. The subject Mathematics will then have two topics, while each topic will have a subtopic.
Call GET on `/subjects/{id}/topics` to list out the topics connected to the subject.  

A GET call to `/topics` will yield both topics and subtopics. The only thing differentiating a topic 
from a subtopic is the connection in `/topic-subtopics`. Similar to the connections between a subject and its topics, you can 
get all subtopics for a topic with a GET call to `/topics/{id}/subtopics`. 


### Updating entities and connections

All PUT calls will overwrite the information in the entity. Be sure to include everything you want to keep. The taxonomy 
API does not check for empty fields unless they are required. The easiest way to update an entity is to first retrieve 
the current entity with a GET call to the correct service and then return the object with a PUT call after you make your changes. 


### Resources 

A Resource (or learning resource) represents an article, a learning path, a video or other content. Its Content URI is 
an ID referring to the actual content which is stored in a different API, e.g., the Article API or the Learning Path API. 

Resources are created in the same way as topics and subjects, but with a POST call to `/resources`. You can connect your Resources 
to Topics by making a POST call to `/topic-resources`. A resource can only be connected to a Subject 
via its Topic(s). You can update a Resource (for instance, change its Content URI) by making a PUT call to `/resources/{id}`. 

List all resources connected to a subject with a GET call to `/subjects/{id}/resources`. For the
Mathematics subject, this call would return a list with these five entities: Tangens, Sine and Cosine, What is probability, 
Adding probabilities, and Probability questions. 

You can also list all resources connected to a topic with a GET call to `/topics/{id}/resources`. If you want to list all 
resources for the Probability topic, you'll get back a list with three resources; What is probability, Adding probabilities 
and Probability questions. 

If you retrieve all resources connected to the topic Statistics, you'll get an empty list, because it doesn't have any 
resources connected directly to it. If you ask for all resources recursively (`/topics/{id}/resources?recursive=true`), 
you'll get the three resources from the Probability topic, since it is a sub topic of Statistics.  


### Resource types

A Resource Type is a category for tagging and filtering Resources. It is a tree structure with two levels. 
In theory, you could make the hierarchy deeper, but that's probably not needed.  

To tag a Resource with Resource Types, first create a Resource Type with a POST call to `/resource-types`. Then 
connect the Resource to the Resource Type with a POST call to `/resource-resourcetypes` including both the ID of the 
Resource and the Resource Type. A resource can have multiple resource types by making several calls. 

When you get all resources for a subject or topic you can choose to get only resources matching a particular resource type 
(or a list of resource types). For our example, a GET call to `/subjects/{id}/resources?type={resourceTypeId}` with `resourceTypeId` 
corresponding to the ID of Articles will give you a list of three entities; Sine and Cosine, What is probability, and Adding probability.


### Multiple parent connections
As some topics such as Statistics may be relevant in several Subjects, multiple parent connections are allowed. This enables you to 
create a structure where Statistics is a topic in Mathematics, but it is also a topic in Social Studies. In these cases,
it makes sense to select Mathematics as the primary Topic. This means that if not specified, Statistics belongs to the context of 
Mathematics. However, if a user is currently browsing the subject of Social Studies, Statistics will still be shown in the context of 
Social Studies. In this regard, the tree behaves a bit like a Unix filesystem, where you may have symbolic and hard links to the same
content. 

![Figure of content structure for mathematics](doc/multiple-parent-connections.png?raw=true)

In the above figure, primary connections are showed in bold red, while the secondary connections are shown in black. Note that a topic or
a resource can only have *one* primary parent. If you set a different connection to be primary, the previous primary connection will become 
secondary.

The figure above shows how Statistics is a topic in both Mathematics and Social Studies. If you list all the topics in 
Social Studies, Statistics will be in the list. It also shows that Riemann Sums is a Resource in both Economics and Integration. If you list all
the Resources in Social Studies or Mathematics, Riemann Sums will be in the list.  

Primary connections are used to determine the default context for a Resource or a Topic. The context of a Resource or a Topic is encoded 
in its URL path, which you get by following the primary connections from the Resource or Topic to the root of the hierarchy.
 
## URLs

Any entity has a URL (e.g., `https://www.ndla.no/subject:1/topic:1/resource:1`), which consists of a scheme (`https`), 
a hostname (`www.ndla.no`) and a path (`/subject:1/topic:1/resource:1`). The taxonomy API only contains information about the path, since 
the path is directly derived from the taxonomy itself. The hostname and scheme are determined by factors outside the bondary of the
taxonomy API. 

The path of any entity is determined by following the connections from it to the root of the hierarchy, picking up the IDs of the 
entities along the way. Using the above figure as an example, we can derive the following paths: 
 
Name                  | ID               | Path(s)
--------------        | ---------------  | ---------------------
Mathematics           | `urn:subject:1`  | `/subject:1`
Social studies        | `urn:subject:2`  | `/subject:2`
Geometry              | `urn:topic:1`    | `/subject:1/topic:1`  
Statistics            | `urn:topic:2`    | `/subject:1/topic:2` `/subject:2/topic:2`
Calculus              | `urn:topic:3`    | `/subject:1/topic:3`
Economics             | `urn:topic:4`    | `/subject:2/topic:4`
Probability           | `urn:topic:5`    | `/subject:1/topic:2/topic:5` `/subject:2/topic:2/topic:5`
Integration           | `urn:topic:6`    | `/subject:1/topic:3/topic:6`
What is probability?  | `urn:resource:1` | `/subject:1/topic:2/topic:5/resource:1` `/subject:2/topic:2/topic:5/resource:1`
Adding probabilities  | `urn:resource:2` | `/subject:1/topic:2/topic:5/resource:2` `/subject:2/topic:2/topic:5/resource:2`
Probability questions | `urn:resource:3` | `/subject:1/topic:2/topic:5/resource:3` `/subject:2/topic:2/topic:5/resource:3`
Riemann sums          | `urn:resource:4` | `/subject:1/topic:3/topic:6/resource:4` `/subject:2/topic:4/resource:4`
 
As you can see, several entities have two paths, since they have more than one path to the root of the hierarchy. The primary path is
listed first. 

### The context of URLs

Say that a user is currently browsing the Social Studies subject. Perhaps the user interface shows a heading indicating that 
Social Studies is the selected subject, there may be breadcrumbs indicating the user's current position in the hierarchy, and 
perhaps a treeview showing the hierarchy below the Social Studies subject. User interfaces may vary, but this imaginary system 
would fall within the norm of such systems. 

Now, say that the user wants to navigate to Statistics, which is a first-level topic within Social Studies. Its primary parent, 
however, is Mathematics. If the link were to transport the user from Social Studies to Mathematics, he would surely be confused. 
So to preserve the current context, we find the possible paths to Statistics, and select the one most closely resembling the 
user's current position in the hierarchy. In this scenario, the possible paths are `/subject:1/topic:2` and `/subject:2/topic:2`. 
Since the user's current position (the current context) is `/subject:2`, we select `/subject:2/topic:2` as the preferred path, since it
has the first nine characters in common with the current context. The other one, `/subject:1/topic:2` has only one. 

When listing entities, the Taxonomy API always includes a path to each entity in the correct context determined by the request. 
Say that you for instance list all topics under Social Studies. Statistics will be included in the result, and its path will be 
`/subject:2/topic:2`. The API has automatically selected the correct path given the context you provided, and returns only that one. 
If you, on the other hand, ask for all topics under Mathematics, Statistics will be listed with a path of `/subject:1/topic:2`. 
The same principle applies for resources. 

