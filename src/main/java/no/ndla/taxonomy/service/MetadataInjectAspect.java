package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Aspect
@Component
public class MetadataInjectAspect {
    private final MetadataApiService metadataApiService;

    private final Map<Class<?>, Set<Method>> setMetadataMethods = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<Field>> allFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<Field>> metadataInjectFields = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<Field>> metadataIdFields = new ConcurrentHashMap<>();

    private final Logger log = LoggerFactory.getLogger(MetadataInjectAspect.class);

    public MetadataInjectAspect(MetadataApiService metadataApiService) {
        this.metadataApiService = metadataApiService;
    }

    private Set<Field> getAllFieldsRecursively(Class<?> clazz) {
        final var fields = new HashSet<Field>();

        //noinspection CollectionAddAllCanBeReplacedWithConstructor
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));

        if (clazz.getSuperclass() != null) {
            fields.addAll(getAllFieldsRecursively(clazz.getSuperclass()));
        }

        return fields;
    }

    private Set<Field> getAllFields(Class<?> clazz) {
        return allFields.computeIfAbsent(clazz, this::getAllFieldsRecursively);
    }

    private Set<Method> getSetMetadataMethodsRecursively(Class<?> clazz) {
        Set<Method> methods = new LinkedHashSet<>();
        for (var method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 1) {
                if (MetadataDto.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    method.setAccessible(true);
                    methods.add(method);
                }
            }
        }

        if (clazz.getSuperclass() != null) {
            methods.addAll(getSetMetadataMethodsRecursively(clazz.getSuperclass()));
        }

        return methods;
    }

    private Set<Method> getSetMetadataMethods(Class<?> clazz) {
        // Searching for a method taking one argument of type MetadataDto which is assumed to be a setter for
        // the metadata object

        return setMetadataMethods.computeIfAbsent(clazz, this::getSetMetadataMethodsRecursively);
    }

    private Set<Field> getInjectMetadataAnnotatedFields(Class<?> clazz) {
        return metadataInjectFields.computeIfAbsent(clazz, n -> {
            final var fields = new HashSet<Field>();

            for (var field : getAllFields(clazz)) {
                if (field.isAnnotationPresent(InjectMetadata.class)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }

            return fields;
        });
    }

    private Set<Field> getMetadataIdAnnotatedFields(Class<?> clazz) {
        return metadataIdFields.computeIfAbsent(clazz, n -> {
            final var fields = new HashSet<Field>();

            for (var field : getAllFields(clazz)) {
                if (field.isAnnotationPresent(MetadataIdField.class)) {
                    if (field.getType().equals(URI.class)) {
                        field.setAccessible(true);
                        fields.add(field);
                    } else {
                        log.warn("Metadata ID mapped field is not an URI");
                    }
                }
            }

            return fields;
        });
    }

    private Map<Class,URI> getMetadataIdsForSingleObject(Object object) {

        return getMetadataIdAnnotatedFields(object.getClass()).stream()
                .map(field -> {
                    try {
                        return Map.entry(field.getDeclaringClass(), (URI) field.get(object));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<URI> getMetadataIds(Object object) {
        // Searches recursively on the object for any metadata IDs (fields with @MetadataIdField) to request for

        final var idList = new LinkedHashSet<URI>();

        if (object instanceof Collection<?>) {

            return ((Collection<?>) object)
                    .stream()
                    .map(this::getMetadataIds)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }

        getMetadataIdsForSingleObject(object)
                .entrySet().stream()
                .map(Map.Entry::getValue)
                .forEach(idList::add);


        getInjectMetadataAnnotatedFields(object.getClass()).forEach(field -> {
            try {
                idList.addAll(getMetadataIds(field.get(object)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        return idList;
    }

    private void injectMultilevelMetadataIntoDto(Object dto, Map<URI, MetadataDto> metadataDtos) {
        // Searches for any field marked with @InjectMetadata and recursively invokes this method on those fields
        // including collections
        for (var field : getInjectMetadataAnnotatedFields(dto.getClass())) {
            try {
                final var value = field.get(dto);

                if (Collection.class.isAssignableFrom(field.getType())) {
                    // Field is a collection, try to inject into all instances of this collection
                    ((Collection<?>) value).forEach(i -> injectMultilevelMetadataIntoDto(i, metadataDtos));
                } else {
                    injectMultilevelMetadataIntoDto(value, metadataDtos);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        // Applies metadata to this object if a setMetadata method is found
        getSetMetadataMethods(dto.getClass()).forEach(setMetadata -> {
            final var metadataIds = getMetadataIdsForSingleObject(dto);
            final URI uri;
            if (metadataIds.containsKey(setMetadata.getDeclaringClass())) {
                uri = metadataIds.get(setMetadata.getDeclaringClass());
            } else {
                uri = metadataIds.entrySet().iterator().next().getValue();
            }
            if (metadataDtos.containsKey(uri)) {
                try {
                    setMetadata.invoke(dto, metadataDtos.get(uri));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void injectMetadataIntoDtos(Object dtos, Map<URI,MetadataDto> metadataDtos) {
        if (dtos instanceof Collection<?>) {
            ((Collection<?>) dtos).forEach(dto -> injectMultilevelMetadataIntoDto(dto, metadataDtos));

            return;
        }

        // Single DTO
        injectMultilevelMetadataIntoDto(dtos, metadataDtos);
    }

    @AfterReturning(value = "@annotation(no.ndla.taxonomy.service.InjectMetadata)", returning = "returnValue")
    public void injectMetadata(Object returnValue) {
        // First collect ALL ids we need to fetch metadata for
        final var idList = getMetadataIds(returnValue);

        // Fetch metadata for the IDs found
        final var metadata = metadataApiService.getMetadataByPublicId(idList);

        // Recursively inject metadata into the returning DTO
        Map<URI,MetadataDto> metadataMap = new LinkedHashMap<>();
        for (MetadataDto m : metadata) {
            try {
                metadataMap.put(new URI(m.getPublicId()), m);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        injectMetadataIntoDtos(returnValue, metadataMap);
    }
}
