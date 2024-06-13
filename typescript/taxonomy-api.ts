
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

export interface GradeAverage extends Serializable {
    averageValue: number;
    count: number;
}

export interface Metadata {
    customFields: Record<string, string>;
    grepCodes: string[];
    visible: boolean;
}

export interface Node {
    baseName: string;
    breadcrumbs: string[];
    contentUri?: string;
    /**
     * An id unique for this context.
     */
    contextId?: string;
    contexts: TaxonomyContext[];
    /**
     * A number representing the average grade of all children nodes recursively.
     */
    gradeAverage?: GradeAverage;
    id: string;
    language: string;
    metadata: Metadata;
    name: string;
    /**
     * The type of node
     */
    nodeType: NodeType;
    path: string;
    paths: string[];
    /**
     * Quality evaluation of the article
     */
    qualityEvaluation?: QualityEvaluation;
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
     * The node is the root in a context. Default is false. Only used if present.
     */
    context?: boolean;
    /**
     * The language used at create time. Used to set default translation.
     */
    language?: string;
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
     * The quality evaluation of the node. Consist of a score from 1 to 5 and a comment.
     */
    qualityEvaluation?: QualityEvaluation | null;
    /**
     * The node is a root node. Default is false. Only used if present.
     * @deprecated
     */
    root?: boolean;
    /**
     * The node is visible. Default is true.
     */
    visible?: boolean;
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

export interface NodeSearchBody {
    /**
     * ContentURIs to fetch for query
     */
    contentUris?: string[];
    customFields?: Record<string, string>;
    /**
     * Filter out programme contexts
     */
    filterProgrammes: boolean;
    /**
     * Ids to fetch for query
     */
    ids?: string[];
    /**
     * Include all contexts
     */
    includeContexts?: boolean;
    /**
     * ISO-639-1 language code
     */
    language?: string;
    /**
     * Filter by nodeType
     */
    nodeType?: NodeType[];
    /**
     * Which page to fetch
     */
    page: number;
    /**
     * How many results to return per page
     */
    pageSize: number;
    /**
     * Query to search names
     */
    query?: string;
}

export interface NodeWithParents extends Node {
    parents: NodeChild[];
}

export interface QualityEvaluation {
    grade: Grade;
    note?: string;
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
    subtypes?: ResourceType[];
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

export interface Serializable {
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
     * A breadcrumb of the names of the context's parents
     */
    breadcrumbs: Record<string, string[]>;
    /**
     * The id of the parent connection object
     */
    connectionId: string;
    /**
     * Unique id of context based on root + parent connection
     */
    contextId: string;
    /**
     * Whether a 'standard'-article, 'topic-article'-article or a 'learningpath'
     */
    contextType?: string;
    /**
     * Whether the parent connection is marked as active
     */
    isActive: boolean;
    /**
     * Whether the parent connection is primary or not
     */
    isPrimary: boolean;
    /**
     * Whether the parent connection is visible or not
     */
    isVisible: boolean;
    /**
     * List of all parent contextIds
     */
    parentContextIds: string[];
    /**
     * List of all parent ids
     */
    parentIds: string[];
    /**
     * The context path
     */
    path: string;
    /**
     * The publicId of the node connected via content-uri
     */
    publicId: string;
    /**
     * The rank of the parent connection object
     */
    rank: number;
    /**
     * Name of the relevance of the parent connection
     */
    relevance: Record<string, string>;
    /**
     * Id of the relevance of the parent connection
     */
    relevanceId: string;
    /**
     * Resource-types of the node
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
     * Pretty-url of this particular context
     */
    url?: string;
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
    id?: string;
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

export type Grade = 1 | 2 | 3 | 4 | 5;

export type NodeType = "NODE" | "SUBJECT" | "TOPIC" | "RESOURCE" | "PROGRAMME";

export type VersionType = "BETA" | "PUBLISHED" | "ARCHIVED";
