# taxonomy-api

[![Build Status](https://travis-ci.org/NDLANO/taxonomy-api.svg?branch=master)](https://travis-ci.org/NDLANO/taxonomy-api)

Rest service and relational database for organizing content.

Are you a developer? Go [here](projectBuild.md) for project build documentation.

## What does this service do?

This API is for organising and categorising the content provided by NDLA. At the heart of the structure are the *resources*, which
represent the actual content, such as articles, videos of lectures, quizzes, learning paths (which are compositions of other 
resources) or any number of other media. The organisation of content allows for a context rich user interface to be built on top
of this API where the content is displayed in context of a *subject* and its *topics*, with hyperlinks to related content. 

Please note that this API is all about metadata. The actual content is stored in other APIs, identified by the content URI
stored with each subject, topic and resource in this API. 

The resources are categorised according to subjects and topics. A subject is a high-level field of study, such as Mathematics
or Science, loosely based on the subject curricula at [udir.no](https://www.udir.no/kl06/MAT1-04?lplang=eng), whereas a topic 
is a hierarchical sub-division of the subject into subject areas. The topics may be loosely based on the subject areas at 
[udir.no](https://www.udir.no/kl06/MAT1-04/Hele/Hovedomraader?lplang=eng), or they may follow a different structure imposed by the
editors.

This organisation gives us a tree representation of the content, where the subjects are at the roots of the tree, the 
topics make up the branches, and the resources are the leaves. Note, however, that this is not a strict tree-structure, since
topics and resources may belong to several parents (see "Multiple parent connections" below). 

The taxonomy data model consists of *entities* and *connections* between entities. 

The entities in the taxonomy are Subject, Topic, Resource, and Resource type. The taxonomy stores metadata for each entity, 
such as name and content URI. Translations of names can also be stored. 

In addition to the entities, the taxonomy stores the connections you make between entities. Each connection also has 
metadata attached to it, such as which entities are connected, and whether or not this connection is the primary connection 
(see "Multiple parent connections" below). Subjects can be connected to topics, topics to subtopics, topics to resources, 
resources to resource types, and resource types to parent resources types. 

Below you can see a figure of how entities can be connected. We will go through how this structure can be realised 
through the API. For details on the use of each service, please see the Swagger documentation. 

![Figure of content structure for mathematics](doc/mathematics-structure.png?raw=true)

### Subjects and topics

First, create a subject with the name Mathematics with a POST call to `/v1/subjects`. When this call returns you'll get a location.
This location contains the path to this subject within the subjects resource, e.g., `/v1/subjects/urn:subject:342`, where `urn:subject:342` 
is the ID of the newly created subject. Any time you need to change or retrieve this subject you'll be using this ID. 

Next, create two Topic entities for Geometry and Statistics (POST to `/v1/topics`). If you have topic descriptions that you want to use
as the content for these entities, you can include their content URI. The content URIs can also be added later (PUT to `/v1/subjects` or `/v1/topics`).

The subject and topic can now be connected with a POST call to `/v1/subject-topics`. 
Use the IDs for the two entities you want to connect. Topics and resources can have multiple parent connections.
The first connection between a subject and a topic will automatically be marked as the primary connection (see "Multiple parent connections" for details).

A topic can have subtopics. In our example Trigonometry is a subtopic of Geometry. To connect the two, create a topic named Trigonometry. 
Then add a connection between the Geometry topic and the Trigonometry topic with a POST call to `/v1/topic-subtopics`. 

The figure above also contains a topic for Statistics and the subtopic Probability. These can be connected in the same 
manner as previously described. The subject Mathematics will then have two topics, while each topic will have a subtopic.
Call GET on `/v1/subjects/{id}/topics` to list out the topics connected to the subject. From this example you will get two topics: Geometry and Statistics. 

A GET call to `/v1/topics` will yield both topics and subtopics. The only thing differentiating a topic 
from a subtopic is the connection in `/v1/topic-subtopics`. Similar to the connections between a subject and its topics, you can 
get all subtopics for a topic with a GET call to `/v1/topics/{id}/subtopics`. 


### Updating entities and connections

Updates to existing entities and connections are all handled with PUT calls to the correct service. Please note that all PUT calls will *overwrite* 
the information in the entity. Be sure to include everything you want to keep. The taxonomy 
API does not check for empty fields unless they are required. The easiest way to update an entity is to first retrieve 
the current entity with a GET call to the correct service and then return the object with a PUT call after you make your changes. The API cannot 
check which fields should be unset, which is why all fields must be present (unless it should be unset). 

You should verify that your changes are correct with a GET call after the PUT request (similarly for POST). This is by design, so 
that the client verifies that the changes on the server are correct. 


### Resources 

A resource (or learning resource) represents an article, a learning path, a video or other content. Its Content URI is 
an ID referring to the actual content which is stored in a different API, e.g., the Article API or the Learning Path API. 

Resources are created in the same way as topics and subjects, but with a POST call to `/v1/resources`. You can connect your resources 
to topics by making a POST call to `/v1/topic-resources`. A resource can only be connected to a subject 
via its topic(s). You can update a resource (for instance, change its Content URI) by making a PUT call to `/v1/resources/{id}`. 

List all resources connected to a subject with a GET call to `/v1/subjects/{id}/resources`. For the
Mathematics subject, this call would return a list with these five entities: Tangens, Sine and Cosine, What is probability, 
Adding probabilities, and Probability questions. 

You can also list all resources connected to a topic with a GET call to `/v1/topics/{id}/resources`. If you want to list all 
resources for the Probability topic, you'll get back a list with three resources; What is probability, Adding probabilities 
and Probability questions. 

If you retrieve all resources connected to the topic Statistics, you'll get an empty list, because it doesn't have any 
resources connected directly to it. If you ask for all resources recursively (`/v1/topics/{id}/resources?recursive=true`), 
you'll get the three resources from the Probability topic, since it is a sub topic of Statistics.  


### Resource types

A resource type is a category for tagging and filtering resources. It is a tree structure with two levels. 
In theory, you could make the hierarchy deeper, but that's probably not needed.  

To tag a resource with resource types, first create a resource type with a POST call to `/v1/resource-types`. Then 
connect the Resource to the resource type with a POST call to `/v1/resource-resourcetypes` including both the ID of the 
resource and the resource type. A resource can have multiple resource types, in that case you make several calls POST to `/v1/resource-types`. 

When you get all resources for a subject or topic you can choose to get only resources matching a particular resource type 
(or a list of resource types). For our example, a GET call to `/v1/subjects/{id}/resources?type={resourceTypeId}` with `resourceTypeId` 
corresponding to the ID of Articles will give you a list of three entities; Sine and Cosine, What is probability, and Adding probability.


### Multiple parent connections
As some topics such as Statistics may be relevant in several subjects, multiple parent connections are allowed. This enables you to 
create a structure where Statistics is a topic in Mathematics, but it is also a topic in Social Studies. In this case,
it makes sense to select Mathematics as the primary topic. This means that if not specified, Statistics belongs to the context of 
Mathematics. However, if a user is currently browsing the subject of Social Studies, Statistics will still be shown in the context of 
Social Studies. In this regard, the tree behaves a bit like a Unix filesystem, where you may have symbolic and hard links to the same
content. 

![Figure of content structure for mathematics](doc/multiple-parent-connections.png?raw=true)

In the figure above, primary connections are showed in bold red while the secondary connections are shown in black. Note that a topic or
a resource can only have *one* primary parent. If you set a different connection to be primary, the previous primary connection will become 
secondary.

The figure above shows how Statistics is a topic in both Mathematics and Social Studies. If you list all the topics in 
Social Studies, Statistics will be in the list. It also shows that Riemann Sums is a resource in both Economics and Integration. If you list all
the resources in Social Studies or Mathematics, Riemann Sums will be in the list.  

If you delete a primary connection to an entity, a new primary connection will be chosen randomly from the remaining connections. If you are 
changing primary connections, you can choose a new primary connection after you have deleted the old, or before.
If an entity no longer has any connections you will not be able to get a URL for that entity (meaning that this 
entity will not be shown in the production system).

Primary connections are used to determine the default context for a resource or a topic. The context of a resource or a topic is encoded 
in its URL path, which you get by following the primary connections from the resource or topic to the root of the hierarchy.
 
## URLs

Entities have URLs (e.g., `https://www.ndla.no/subject:1/topic:1/resource:1`) which consists of a scheme (`https`), 
a hostname (`www.ndla.no`), and a path (`/subject:1/topic:1/resource:1`). The taxonomy API only contains information about the path, since 
the path is directly derived from the taxonomy itself. The hostname and scheme are determined by factors outside the boundary of the
taxonomy API. When you perform a GET call on an entity (using the entity ID), you will also get the primary path to the entity along with 
the name and content URI.

The taxonomy API generates paths for all entities. The path of an entity is determined by following the connections from 
it to the root of the hierarchy, picking up the IDs of the 
entities along the way. Entities without connections do not have URLs (except subjects, which are at the root of the hierarchy). 
Using the above figure as an example, we can derive the following paths: 


| Name                  | ID               | Path(s)                                                                         | 
|-----------------------|------------------|---------------------------------------------------------------------------------|
| Mathematics           | `urn:subject:1`  | `/subject:1`                                                                    |
| Social studies        | `urn:subject:2`  | `/subject:2`                                                                    |
| Geometry              | `urn:topic:1`    | `/subject:1/topic:1`                                                            |
| Statistics            | `urn:topic:2`    | `/subject:1/topic:2` `/subject:2/topic:2`                                       |
| Calculus              | `urn:topic:3`    | `/subject:1/topic:3`                                                            |
| Economics             | `urn:topic:4`    | `/subject:2/topic:4`                                                            |
| Probability           | `urn:topic:5`    | `/subject:1/topic:2/topic:5` `/subject:2/topic:2/topic:5`                       |
| Integration           | `urn:topic:6`    | `/subject:1/topic:3/topic:6`                                                    |
| What is probability?  | `urn:resource:1` | `/subject:1/topic:2/topic:5/resource:1` `/subject:2/topic:2/topic:5/resource:1` |
| Adding probabilities  | `urn:resource:2` | `/subject:1/topic:2/topic:5/resource:2` `/subject:2/topic:2/topic:5/resource:2` |
| Probability questions | `urn:resource:3` | `/subject:1/topic:2/topic:5/resource:3` `/subject:2/topic:2/topic:5/resource:3` |
| Riemann sums          | `urn:resource:4` | `/subject:1/topic:3/topic:6/resource:4` `/subject:2/topic:4/resource:4`         |


As you can see, several entities have two paths, since they have more than one path to the root of the hierarchy. The primary path is
listed first. 

### The context of URLs

Say that a user is currently browsing the Social Studies subject. Perhaps the user interface shows a heading indicating that 
Social Studies is the selected subject, there may be breadcrumbs indicating the user's current position in the hierarchy, and 
perhaps a treeview showing the hierarchy below the Social Studies subject. User interfaces may vary, but this imaginary system 
would fall within the norm of such systems. 

Now, say that the user wants to navigate to Statistics, which is a first-level topic within Social Studies. Its primary parent, 
however, is Mathematics. If the link were to transport the user from the subejct of Social Studies to Mathematics, he would surely be confused. 
So to preserve the current context, the API finds the possible paths to Statistics, and selects the one most closely resembling the 
user's current position in the hierarchy. In this scenario, the possible paths are `/subject:1/topic:2` and `/subject:2/topic:2`. 
Since the user's current position (the current context) is `/subject:2`, we select `/subject:2/topic:2` as the preferred path, since it
has the first nine characters in common with the current context. The other one, `/subject:1/topic:2` has only one. 

When listing entities, the Taxonomy API always includes a path to each entity in the correct context determined by the request. 
Say that you for instance list all topics under Social Studies. Statistics will be included in the result, and its path will be 
`/subject:2/topic:2`. The API has automatically selected the correct path given the context you provided, and returns only that one. 
If you, on the other hand, ask for all topics under Mathematics, Statistics will be listed with a path of `/subject:1/topic:2`. 
The same principle applies for resources. 

### Topics as root contexts

In the above examples, all paths start with a subject. But sometimes, you encounter a topic which is important enough that it should
be an entry point into the taxonomy in its own right, without being a child of a particular subject. Examples of such topics could
be current events that should have greater visibility for a limited time period, such as the US Presidential Election 2016. 
Other examples may be strong topics that are always important in several subjects and nearly a subject in itself, such as Statistics. 
 
When you mark a topic as a root context, you allow URLs to start with the id of that topic. In the above example, Statistics 
is a child of both Mathematics and Social Studies, so it is accessible through the paths `/subject:1/topic:2` and `/subject:2/topic:2`. 
If we add Statistics as a root context, it will also be accessible through the path `/topic:2` directly. The same goes for any
sub topics and resources below Statistics:


| Name                  | ID               | Path(s)                                                                                                       | 
|-----------------------|------------------|---------------------------------------------------------------------------------------------------------------|
| Mathematics           | `urn:subject:1`  | `/subject:1`                                                                                                  |
| Social studies        | `urn:subject:2`  | `/subject:2`                                                                                                  |
| Statistics            | `urn:topic:2`    | `/topic:2` `/subject:1/topic:2` `/subject:2/topic:2`                                                          |
| Probability           | `urn:topic:5`    | `/topic:2/topic:5` `/subject:1/topic:2/topic:5` `/subject:2/topic:2/topic:5`                                  |
| What is probability?  | `urn:resource:1` | `/topic:2/topic:5/resource:1` `/subject:1/topic:2/topic:5/resource:1` `/subject:2/topic:2/topic:5/resource:1` |
| Adding probabilities  | `urn:resource:2` | `/topic:2/topic:5/resource:2` `/subject:1/topic:2/topic:5/resource:2` `/subject:2/topic:2/topic:5/resource:2` |
| Probability questions | `urn:resource:3` | `/topic:2/topic:5/resource:3` `/subject:1/topic:2/topic:5/resource:3` `/subject:2/topic:2/topic:5/resource:3` |

 
A topic can be marked as a root context by making a POST call to `/v1/contexts`, and removed by making a DELETE call to the same.  
All subjects are root contexts. Please note that POST and DELETE calls to `/v1/contexts` does not create or remove topics, it only
marks those topics as being a root context or not. 

To list all root contexts, make a GET call to `/v1/contexts`. The contexts will be listed with their ID, (translated) name and path. 
 
### Translations

When an entity is created, the textual information entered (such as name) becomes the default translation. Add other 
translations for a subject with a PUT call to `/v1/subjects/{id}/translations/{language}` where language is the two letter 
ISO 639-1 language code (equivalent call can be made for all entities). 

When getting one or several entities you can request a specific translation. If the translation exists, the object you 
get back will have the correct translation of name (or any other relevant fields). If you request all resources under a 
subject or topic, all entities that have this translation will use it. The other entities will use the default translation. 
In this way the complete set of resources belonging to a topic or subject will be returned even if the translation the 
user has requested does not exist.
 
You can also get all translations for an entity. Get all available translations with a topic with a GET call to `/v1/topics/{id}/translations`. 


## Filters

The topics and learning resources contained in a subject may be organised in a way that spans academic years and 
academic programs. Mathematics would, for instance, contain a topic called geometry, which contains several learning 
resources. Some of these are appropriate for first-year students, while some are more advanced and more suited for 
second-year students. Additionally, some learning resources may be considered *core material* for second-year students, 
but may be offered as *supplementary material* to first-year students who wish to delve deeper. 

![Filters in mathematics](doc/filters.png?raw=true)

In the above example, *right triangles* and *pythagoras* are both considered *core material* in the *R1* program, while
*trigonometry* is *core material* in the *R2* program. Additionally, *trigonometry* is supplementary material in R1.
All resources under *algebra* is core material in R1. 

Using this data structure it is possible to limit the resources shown in *Mathematics* to find resources that are relevant
to a given academic program or academic year: 

#### Example: List core material in Mathematics for R2

The results will contain *trigonometry*, since this is tagged as core material in R2. 

#### Example: List core and supplementary material in Mathematics for R1

The results will contain *Equations with one variable*, *Equations with two variables*, *Right triangles*, *Pythagoras* 
and *Trigonometry*, since these are either tagged as core or supplementary material in R1, or is contained in a topic
which is tagged as core or supplementary material in R1. 

#### Example: List core material in Mathematics

The results will contain *Equations with one variable*, *Equations with two variables*, *Right triangles*, *Pythagoras* 
and *Trigonometry*, since these are tagged as core material in either R1 or R2

#### Example: List all resources in Mathematics

The results will contain *Equations with one variable*, *Equations with two variables*, *Right triangles*, *Pythagoras*, 
*Trigonometry* and *Right triangles* since all of these resources are contained in a topic connected to Mathematics. 
                       
Note that the resource *Right triangles* is not marked as either core or supplementary material in neither R1 nor R2. 
When listing all resources in R1 and R2, this resource will not be included. It will, however, be included when listing
all resources in Mathematics.

### Tagging for multiple subjects

![Filters in mathematics](doc/filters.png?raw=true)

If a resource is used in several subjects, you can tag it as core or supplementary material in each of the subjects
separately. In the example above, trigonometry is tagged as core material in Mathematics R2, and supplementary material
in Mathematics R1 and Construction VG2. 
 
### Creating filters

A filter belongs to one and only one subject, and resources must be associated with the filter to be included when listing
the contents of a subject when the filter is activated. The resources can either be tagged with the filter directly, or
by tagging a containing topic with the filter. 

To create a filter, first ensure that the subject it will belong to is already created. You can then make a POST request to 
`/v1/filters`, which must include the subject id. A filter may be modified (or associated with a different subject) later on
by making a PUT request to `/v1/filters/{id}`. 

When associating a filter with a resource, you must also indicate what relevance that resource has in context of the filter. 
This is done by first creating an instance of *Relevance* by making a POST request to `/v1/relevances`. It is recommended that 
you provide a programmer-friendly ID when creating a relevance, e.g., `urn:relevance:core` if the relevance is called 
"Core material". This will make it easier to distinguish different relevances in display logic. 
 
After creating the filter, you can associate a resource to it by making a POST request to `/v1/resource-filters`, including 
the ids of both the filter, the resource and the relevance. This three-way association can be edited by making a PUT request
to `/v1/resouce-filters/{id}` later on. Note that you cannot change the filter or the resource, but you can change the 
relevance. If you need to change the filter this resource is associated with, delete this association and make a new one.  

### Using filters

When listing the contents of a subject, you can use one or more filter ids to limit the results. If you make a GET request
to either `/v1/subjects/{id}/topics` or `/v1/subjects/{id}/resources`, you will get all topics or resources associated with 
that subject. To limit the results based on filter, add the query string `?filter={filter id}`. You may repeat the query
string to include results from several filters, or you may separate ids with comma. 

You can also limit results by relevance by adding `?relevance={relevance id}` to the query string. When combining filters 
and relevance in the same query, the effect is similar to this: `(filter 1 OR filter 2) AND (relevance 1 OR relevance 2)`.
If you need more precision, you must make several queries. 

