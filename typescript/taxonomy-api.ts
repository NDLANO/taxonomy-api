
export interface Connection {
    connectionId: string;
    isPrimary: boolean;
    paths: string[];
    targetId: string;
    type: string;
}

export interface Context {
    id: string;
    name: string;
    path: string;
}

export interface ContextPOST {
    id: string;
}

export interface Metadata {
    customFields: Record<string, string>;
    grepCodes: string[];
    visible: boolean;
}

export interface Node {
    breadcrumbs: string[];
    contentUri?: string;
    /**
     * An id unique for this context.
     */
    contextId?: string;
    contexts: TaxonomyContext[];
    id: string;
    metadata: Metadata;
    name: string;
    /**
     * The type of node
     */
    nodeType: NodeType;
    path: string;
    paths: string[];
    relevanceId?: string;
    resourceTypes: ResourceTypeWithConnection[];
    supportedLanguages: string[];
    translations: Translation[];
    url?: string;
}

export interface NodeChild extends Node {
    connectionId: string;
    isPrimary: boolean;
    /**
     * @deprecated
     */
    parent: string;
    parentId: string;
    rank: number;
}

export interface NodeConnection {
    /**
     * Child id
     */
    childId: string;
    /**
     * Connection id
     */
    id: string;
    /**
     * Metadata for entity. Read only.
     */
    metadata: Metadata;
    /**
     * Parent id
     */
    parentId: string;
    /**
     * Is this connection primary
     */
    primary: boolean;
    /**
     * Order in which subtopic is sorted for the topic
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
}

export interface NodeConnectionPOST {
    /**
     * Child id
     */
    childId: string;
    parentId: string;
    /**
     * If this connection is primary.
     */
    primary?: boolean;
    /**
     * Order in which to sort the child for the parent
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
}

export interface NodeConnectionPUT {
    /**
     * If this connection is primary.
     */
    primary?: boolean;
    /**
     * Order in which subtopic is sorted for the topic
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
}

export interface NodePostPut {
    /**
     * ID of content introducing this node. Must be a valid URI, but preferably not a URL.
     */
    contentUri?: string;
    /**
     * The name of the node. Required on create.
     */
    name?: string;
    nodeId?: string;
    /**
     * Type of node.
     */
    nodeType: NodeType;
    /**
     * The node is a root node. Default is false. Only used if present.
     */
    root?: boolean;
}

export interface NodeResource {
    /**
     * Node resource connection id
     */
    id: string;
    /**
     * Metadata for entity. Read only.
     */
    metadata: Metadata;
    /**
     * Node id
     */
    nodeId: string;
    /**
     * Primary connection
     */
    primary: boolean;
    /**
     * Order in which the resource is sorted for the node
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Resource id
     */
    resourceId: string;
}

export interface NodeResourcePOST {
    /**
     * Node id
     */
    nodeId: string;
    /**
     * Primary connection
     */
    primary?: boolean;
    /**
     * Order in which resource is sorted for the node
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Resource id
     */
    resourceId: string;
}

export interface NodeResourcePUT {
    /**
     * Primary connection
     */
    primary?: boolean;
    /**
     * Order in which the resource will be sorted for this node.
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
}

export interface NodeWithParents extends Node {
    parents: NodeChild[];
}

export interface Relevance {
    /**
     * Specifies if node is core or supplementary
     */
    id: string;
    /**
     * The name of the relevance
     */
    name: string;
    /**
     * List of language codes supported by translations
     */
    supportedLanguages: string[];
    /**
     * All translations of this relevance
     */
    translations: Translation[];
}

export interface RelevancePUT {
    /**
     * If specified, set the id to this value. Must start with urn:relevance: and be a valid URI. If ommitted, an id will be assigned automatically. Ignored on update
     */
    id: string;
    /**
     * The name of the relevance
     */
    name: string;
}

export interface ResolvedOldUrl {
    /**
     * URL path for resource
     */
    path: string;
}

export interface ResolvedUrl {
    contentUri: string;
    id: string;
    name: string;
    parents: string[];
    path: string;
}

export interface ResourcePostPut {
    /**
     * The ID of this resource in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier for the system, and <id> is the id of this content in that system.
     */
    contentUri: string;
    /**
     * If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.
     */
    id?: string;
    /**
     * The name of the resource
     */
    name: string;
}

export interface ResourceResourceType {
    /**
     * Resource to resource type connection id
     */
    id: string;
    /**
     * Resource type id
     */
    resourceId: string;
    /**
     * Resource type id
     */
    resourceTypeId: string;
}

export interface ResourceResourceTypePOST {
    /**
     * Resource id
     */
    resourceId: string;
    /**
     * Resource type id
     */
    resourceTypeId: string;
}

export interface ResourceType {
    id: string;
    /**
     * The name of the resource type
     */
    name: string;
    /**
     * Sub resource types
     */
    subtypes: ResourceType[];
    /**
     * List of language codes supported by translations
     */
    supportedLanguages: string[];
    /**
     * All translations of this resource type
     */
    translations: Translation[];
}

export interface ResourceTypePUT {
    /**
     * If specified, set the id to this value. Must start with urn:resourcetype: and be a valid URI. If omitted, an id will be assigned automatically.
     */
    id: string;
    /**
     * The name of the resource type
     */
    name: string;
    /**
     * If specified, the new resource type will be a child of the mentioned resource type.
     */
    parentId: string;
}

export interface ResourceTypeWithConnection {
    connectionId: string;
    id: string;
    name: string;
    parentId?: string;
    /**
     * List of language codes supported by translations
     */
    supportedLanguages: string[];
    translations: Translation[];
}

export interface SearchResult<T> {
    page: number;
    pageSize: number;
    results: T[];
    totalCount: number;
}

export interface SearchableTaxonomyResourceType {
    id: string;
    name: Record<string, string>;
}

export interface SubjectPostPut {
    /**
     * ID of frontpage connected to this subject. Must be a valid URI, but preferably not a URL.
     */
    contentUri?: string;
    /**
     * If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.
     */
    id?: string;
    /**
     * The name of the subject
     */
    name: string;
}

export interface SubjectTopic {
    /**
     * Connection id
     */
    id: string;
    /**
     * primary
     */
    primary: boolean;
    /**
     * Order in which the topic is sorted under the subject
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Subject id
     */
    subjectid: string;
    /**
     * Topic id
     */
    topicid: string;
}

export interface SubjectTopicPOST {
    /**
     * Backwards compatibility: Always true, ignored on insert/update.
     */
    primary?: boolean;
    /**
     * Order in which the topic should be sorted for the topic
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Subject id
     */
    subjectid: string;
    /**
     * Topic id
     */
    topicid: string;
}

export interface SubjectTopicPUT {
    /**
     * If true, set this subject as the primary subject for this topic. This will replace any other primary subject for this topic. You must have one primary subject, so it is not allowed to set the currently primary subject to not be primary any more.
     */
    primary?: boolean;
    /**
     * Order in which the topic should be sorted for the subject
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
}

export interface TaxonomyContext {
    /**
     * A breadcrumb of the names of the context's path
     */
    breadcrumbs: Record<string, string[]>;
    /**
     * Unique id of context based on root + connection
     */
    contextId: string;
    /**
     * Whether a 'standard'-article, 'topic-article'-article or a 'learningpath'
     */
    contextType?: string;
    /**
     * @deprecated
     */
    id?: string;
    /**
     * Whether the base connection is marked as active subject
     */
    isActive: boolean;
    /**
     * Whether the base connection is primary or not
     */
    isPrimary: boolean;
    /**
     * @deprecated
     */
    isPrimaryConnection?: boolean;
    /**
     * Whether the base connection is visible or not
     */
    isVisible: boolean;
    /**
     * List of all parent topic-ids
     */
    parentIds: string[];
    /**
     * @deprecated
     */
    parentTopicIds?: string[];
    /**
     * The context path
     */
    path: string;
    /**
     * The publicId of the node connected via content-uri
     */
    publicId: string;
    /**
     * Name of the relevance of the connection of the base
     */
    relevance: Record<string, string>;
    /**
     * Id of the relevance of the connection of the base
     */
    relevanceId?: string;
    /**
     * Resource-types of the base
     */
    resourceTypes: SearchableTaxonomyResourceType[];
    /**
     * The name of the root parent of the context
     */
    root: Record<string, string>;
    /**
     * The publicId of the root parent of the context
     */
    rootId: string;
    /**
     * @deprecated
     */
    subject?: Record<string, string>;
    /**
     * @deprecated
     */
    subjectId?: string;
}

export interface TopicPostPut {
    /**
     * ID of article introducing this topic. Must be a valid URI, but preferably not a URL.
     */
    contentUri: string;
    /**
     * If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.
     */
    id?: string;
    /**
     * The name of the topic
     */
    name: string;
}

export interface TopicResource {
    /**
     * Topic resource connection id
     */
    id: string;
    /**
     * Primary connection
     */
    primary: boolean;
    /**
     * Order in which the resource is sorted for the topic
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Resource id
     */
    resourceId: string;
    /**
     * Topic id
     */
    topicid: string;
}

export interface TopicResourcePOST {
    /**
     * Primary connection
     */
    primary?: boolean;
    /**
     * Order in which resource is sorted for the topic
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Resource id
     */
    resourceId: string;
    /**
     * Topic id
     */
    topicid: string;
}

export interface TopicResourcePUT {
    /**
     * Primary connection
     */
    primary?: boolean;
    /**
     * Order in which the resource will be sorted for this topic.
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
}

export interface TopicSubtopic {
    /**
     * Connection id
     */
    id: string;
    /**
     * Backwards compatibility: Always true. Ignored on insert/update
     */
    primary: boolean;
    /**
     * Order in which subtopic is sorted for the topic
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Subtopic id
     */
    subtopicid: string;
    /**
     * Topic id
     */
    topicid: string;
}

export interface TopicSubtopicPOST {
    /**
     * Is this connection primary
     */
    primary?: boolean;
    /**
     * Order in which to sort the subtopic for the topic
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
    /**
     * Subtopic id
     */
    subtopicid: string;
    /**
     * Topic id
     */
    topicid: string;
}

export interface TopicSubtopicPUT {
    /**
     * Is this connection primary
     */
    primary?: boolean;
    /**
     * Order in which subtopic is sorted for the topic
     */
    rank?: number;
    /**
     * Relevance id
     */
    relevanceId?: string;
}

export interface Translation {
    /**
     * ISO 639-1 language code
     */
    language: string;
    /**
     * The translated name of the node
     */
    name: string;
}

export interface TranslationPUT {
    /**
     * The translated name of the element. Used wherever translated texts are used.
     */
    name: string;
}

export interface UrlMapping {
    /**
     * Node URN for resource in new system
     */
    nodeId: string;
    /**
     * Subject URN for resource in new system (optional)
     */
    subjectId: string;
    /**
     * URL for resource in old system
     */
    url: string;
}

export interface Version {
    archived?: DateAsString;
    created: DateAsString;
    hash: string;
    id: string;
    /**
     * Is the version locked
     */
    locked: boolean;
    name: string;
    published?: DateAsString;
    versionType: VersionType;
}

export interface VersionPostPut {
    /**
     * If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.
     */
    id: string;
    /**
     * If specified, set the locked property to this value.
     */
    locked?: boolean;
    /**
     * If specified, set the name to this value.
     */
    name: string;
}

export type DateAsString = string;

export type NodeType = "NODE" | "SUBJECT" | "TOPIC" | "RESOURCE";

export type VersionType = "BETA" | "PUBLISHED" | "ARCHIVED";
