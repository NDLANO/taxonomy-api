/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2023-05-02 07:54:10.

export interface Contexts {
}

export interface Context {
    id: string;
    path: string;
    name: string;
}

export interface ContextPOST {
    id: string;
}

export interface CrudController<T> {
}

export interface CrudControllerWithMetadata<T> extends CrudController<T> {
}

export interface NodeConnections extends CrudControllerWithMetadata<any> {
    allNodeConnections: NodeConnection[];
}

export interface NodeConnectionPOST {
    parentId: string;
    /**
     * Child id
     */
    childId: string;
    /**
     * Backwards compatibility: Always true. Ignored on insert/update
     */
    primary: boolean;
    /**
     * Order in which to sort the child for the parent
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

export interface NodeConnectionPUT {
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
    relevanceId: string;
}

export interface NodeResources extends CrudControllerWithMetadata<any> {
    allNodeResources: NodeResource[];
}

export interface NodeResourcePOST {
    /**
     * Node id
     */
    nodeId: string;
    /**
     * Resource id
     */
    resourceId: string;
    /**
     * Primary connection
     */
    primary: boolean;
    /**
     * Order in which resource is sorted for the node
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

export interface NodeResourcePUT {
    /**
     * Node resource connection id
     */
    id: string;
    /**
     * Primary connection
     */
    primary: boolean;
    /**
     * Order in which the resource will be sorted for this node.
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

export interface NodeTranslations {
}

export interface Translation {
    /**
     * The translated name of the node
     */
    name: string;
    /**
     * ISO 639-1 language code
     */
    language: string;
}

export interface TranslationPUT {
    /**
     * The translated name of the node
     */
    name: string;
}

export interface Nodes extends CrudControllerWithMetadata<any> {
}

export interface Queries {
}

export interface Relevances extends CrudController<any> {
}

export interface Relevance {
    id: string;
    /**
     * The name of the relevance
     */
    name: string;
    /**
     * All translations of this relevance
     */
    translations: Translation[];
    /**
     * List of language codes supported by translations
     */
    supportedLanguages: string[];
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

export interface ResourceResourceTypes {
    allResourceResourceTypes: ResourceResourceType[];
}

export interface ResourceResourceType {
    /**
     * Resource type id
     */
    resourceId: string;
    /**
     * Resource type id
     */
    resourceTypeId: string;
    /**
     * Resource to resource type connection id
     */
    id: string;
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

/**
 * @deprecated
 */
export interface ResourceTranslations {
}

export interface ResourceTranslation {
    /**
     * The translated name of the resource
     */
    name: string;
    /**
     * ISO 639-1 language code
     */
    language: string;
}

export interface ResourceTranslationPUT {
    /**
     * The translated name of the resource
     */
    name: string;
}

export interface ResourceTypeTranslations {
}

export interface ResourceTypeTranslation {
    /**
     * The translated name of the resource type
     */
    name: string;
    /**
     * ISO 639-1 language code
     */
    language: string;
}

export interface ResourceTypeTranslationPUT {
    /**
     * The translated name of the resource type
     */
    name: string;
}

export interface ResourceTypes extends CrudController<any> {
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
     * All translations of this resource type
     */
    translations: Translation[];
    /**
     * List of language codes supported by translations
     */
    supportedLanguages: string[];
}

export interface ResourceTypePUT {
    /**
     * If specified, the new resource type will be a child of the mentioned resource type.
     */
    parentId: string;
    /**
     * If specified, set the id to this value. Must start with urn:resourcetype: and be a valid URI. If omitted, an id will be assigned automatically.
     */
    id: string;
    /**
     * The name of the resource type
     */
    name: string;
}

/**
 * @deprecated
 */
export interface Resources extends CrudControllerWithMetadata<any> {
}

/**
 * @deprecated
 */
export interface SubjectTopics {
    allSubjectTopics: SubjectTopic[];
}

export interface SubjectTopic {
    /**
     * Subject id
     */
    subjectid: string;
    /**
     * Topic id
     */
    topicid: string;
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
    relevanceId: string;
}

export interface SubjectTopicPOST {
    /**
     * Subject id
     */
    subjectid: string;
    /**
     * Topic id
     */
    topicid: string;
    /**
     * Backwards compatibility: Always true, ignored on insert/update.
     */
    primary: boolean;
    /**
     * Order in which the topic should be sorted for the topic
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

export interface SubjectTopicPUT {
    /**
     * connection id
     */
    id: string;
    /**
     * If true, set this subject as the primary subject for this topic. This will replace any other primary subject for this topic. You must have one primary subject, so it is not allowed to set the currently primary subject to not be primary any more.
     */
    primary: boolean;
    /**
     * Order in which the topic should be sorted for the subject
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

/**
 * @deprecated
 */
export interface SubjectTranslations {
}

export interface SubjectTranslation {
    /**
     * The translated name of the subject
     */
    name: string;
    /**
     * ISO 639-1 language code
     */
    language: string;
}

export interface SubjectTranslationPUT {
    /**
     * The translated name of the subject
     */
    name: string;
}

/**
 * @deprecated
 */
export interface Subjects extends CrudControllerWithMetadata<any> {
}

/**
 * @deprecated
 */
export interface TopicResources {
    allTopicResources: TopicResource[];
}

export interface TopicResource {
    /**
     * Topic id
     */
    topicid: string;
    /**
     * Resource id
     */
    resourceId: string;
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
    relevanceId: string;
}

export interface TopicResourcePOST {
    /**
     * Topic id
     */
    topicid: string;
    /**
     * Resource id
     */
    resourceId: string;
    /**
     * Primary connection
     */
    primary: boolean;
    /**
     * Order in which resource is sorted for the topic
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

export interface TopicResourcePUT {
    /**
     * Topic resource connection id
     */
    id: string;
    /**
     * Primary connection
     */
    primary: boolean;
    /**
     * Order in which the resource will be sorted for this topic.
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

/**
 * @deprecated
 */
export interface TopicSubtopics {
    allTopicSubtopics: TopicSubtopic[];
}

export interface TopicSubtopic {
    /**
     * Topic id
     */
    topicid: string;
    /**
     * Subtopic id
     */
    subtopicid: string;
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
    relevanceId: string;
}

export interface TopicSubtopicPOST {
    /**
     * Topic id
     */
    topicid: string;
    /**
     * Subtopic id
     */
    subtopicid: string;
    /**
     * Backwards compatibility: Always true. Ignored on insert/update
     */
    primary: boolean;
    /**
     * Order in which to sort the subtopic for the topic
     */
    rank: number;
    /**
     * Relevance id
     */
    relevanceId: string;
}

export interface TopicSubtopicPUT {
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
    relevanceId: string;
}

/**
 * @deprecated
 */
export interface TopicTranslations {
}

export interface TopicTranslation {
    /**
     * The translated name of the topic
     */
    name: string;
    /**
     * ISO 639-1 language code
     */
    language: string;
}

export interface TopicTranslationPUT {
    /**
     * The translated name of the topic
     */
    name: string;
}

/**
 * @deprecated
 */
export interface Topics extends CrudControllerWithMetadata<any> {
}

export interface UrlResolver {
}

export interface ResolvedOldUrl {
    /**
     * URL path for resource
     */
    path: string;
}

export interface UrlMapping {
    /**
     * URL for resource in old system
     */
    url: string;
    /**
     * Node URN for resource in new system
     */
    nodeId: string;
    /**
     * Subject URN for resource in new system (optional)
     */
    subjectId: string;
}

export interface Versions extends CrudController<any> {
}

export interface NodeCommand {
    nodeId?: string;
    /**
     * Type of node. Values are subject, topic. Required on create.
     */
    nodeType: any;
    /**
     * ID of content introducing this node. Must be a valid URI, but preferably not a URL.
     */
    contentUri: string;
    /**
     * The name of the node. Required on create.
     */
    name: string;
    /**
     * The node is a root node. Default is false. Only used if present.
     */
    root: boolean;
}

export interface ResourceCommand {
    /**
     * If specified, set the id to this value. Must start with urn:resource: and be a valid URI. If omitted, an id will be assigned automatically.
     */
    id: string;
    /**
     * The ID of this resource in the system where the content is stored. This ID should be of the form 'urn:<system>:<id>', where <system> is a short identifier for the system, and <id> is the id of this content in that system.
     */
    contentUri: string;
    /**
     * The name of the resource
     */
    name: string;
}

export interface SubjectCommand {
    /**
     * If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.
     */
    id: string;
    /**
     * ID of article introducing this subject. Must be a valid URI, but preferably not a URL.
     */
    contentUri: string;
    /**
     * The name of the subject
     */
    name: string;
}

export interface TopicCommand {
    /**
     * If specified, set the id to this value. Must start with urn:topic: and be a valid URI. If omitted, an id will be assigned automatically.
     */
    id: string;
    /**
     * ID of article introducing this topic. Must be a valid URI, but preferably not a URL.
     */
    contentUri: string;
    /**
     * The name of the topic
     */
    name: string;
}

export interface VersionCommand {
    /**
     * If specified, set the id to this value. Must start with urn:subject: and be a valid URI. If ommitted, an id will be assigned automatically.
     */
    id: string;
    /**
     * If specified, set the name to this value.
     */
    name: string;
    /**
     * If specified, set the locked property to this value.
     */
    locked: boolean;
}

export interface NodeConnection {
    /**
     * Parent id
     */
    parentId: string;
    /**
     * Child id
     */
    childId: string;
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
    relevanceId: string;
    /**
     * Metadata for entity. Read only.
     */
    metadata: Metadata;
}

export interface NodeResource {
    /**
     * Node id
     */
    nodeId: string;
    /**
     * Resource id
     */
    resourceId: string;
    /**
     * Node resource connection id
     */
    id: string;
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
    relevanceId: string;
    /**
     * Metadata for entity. Read only.
     */
    metadata: Metadata;
}

export interface SearchableTaxonomyResourceType {
    id: string;
    name: Record<string, string>;
}

export interface TaxonomyContext {
    /**
     * The publicId of the node connected via content-uri
     */
    publicId: string;
    /**
     * The publicId of the root parent of the context
     */
    rootId: string;
    /**
     * The name of the root parent of the context
     */
    root: Record<string, string>;
    /**
     * The context path
     */
    path: string;
    /**
     * A breadcrumb of the names of the context's path
     */
    breadcrumbs: Record<string, string[]>;
    /**
     * Whether a 'standard'-article, 'topic-article'-article or a 'learningpath'
     */
    contextType?: string;
    /**
     * Id of the relevance of the connection of the base
     */
    relevanceId: string;
    /**
     * Name of the relevance of the connection of the base
     */
    relevance: Record<string, string>;
    /**
     * Resource-types of the base
     */
    resourceTypes: SearchableTaxonomyResourceType[];
    /**
     * List of all parent topic-ids
     */
    parentIds: string[];
    /**
     * Whether the base connection is primary or not
     */
    isPrimary: boolean;
    /**
     * Whether the base connection is visible or not
     */
    isVisible: boolean;
    /**
     * Unique id of context based on root + connection
     */
    contextId: string;
    id: string;
    subject: Record<string, string>;
    subjectId: string;
    parentTopicIds: string[];
    isPrimaryConnection: boolean;
}

export interface Connection {
    connectionId: string;
    targetId: string;
    paths: string[];
    type: string;
    /**
     * True if owned by this topic, false if it has its primary connection elsewhere
     */
    isPrimary: boolean;
    primary: boolean;
}

export interface Metadata {
    grepCodes: string[];
    visible: boolean;
    customFields: Record<string, string>;
}

export interface NodeChild extends Node {
    parentId: string;
    connectionId: string;
    rank: number;
    parent: string;
    primary: boolean;
}

export interface Node {
    id: string;
    name: string;
    contentUri: string;
    path: string;
    paths: string[];
    metadata: Metadata;
    relevanceId: string;
    translations: Translation[];
    supportedLanguages: string[];
    breadcrumbs: string[];
    resourceTypes: ResourceTypeWithConnection[];
    /**
     * The type of node
     */
    nodeType: any;
    /**
     * An id unique for this context.
     */
    contextId: string;
    url?: string;
    contexts: TaxonomyContext[];
}

export interface NodeWithParents extends Node {
    parents: NodeChild[];
}

export interface ResolvedUrl {
    id: string;
    contentUri: string;
    name: string;
    parents: string[];
    path: string;
}

export interface ResourceTypeWithConnection {
    id: string;
    parentId: string;
    name: string;
    translations: Translation[];
    /**
     * List of language codes supported by translations
     */
    supportedLanguages: string[];
    connectionId: string;
}

export interface SearchResult<T> {
    totalCount: number;
    page: number;
    pageSize: number;
    results: T[];
}

export interface Version {
    id: string;
    versionType: any;
    name: string;
    hash: string;
    /**
     * Is the version locked
     */
    locked: boolean;
    created: string;
    published: string;
    archived: string;
}
