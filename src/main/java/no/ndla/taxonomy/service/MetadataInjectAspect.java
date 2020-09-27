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
import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
public class MetadataInjectAspect {
    private final MetadataApiService metadataApiService;

    private final Map<Class<?>, Optional<Method>> setMetadataMethods = new HashMap<>();
    private final Map<Class<?>, Set<Field>> allFields = new HashMap<>();
    private final Map<Class<?>, Set<Field>> metadataInjectFields = new HashMap<>();
    private final Map<Class<?>, Set<Field>> metadataIdFields = new HashMap<>();

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

    private Optional<Method> getSetMetadataMethodRecursively(Class<?> clazz) {
        for (var method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() == 1) {
                if (MetadataDto.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    return Optional.of(method);
                }
            }
        }

        if (clazz.getSuperclass() != null) {
            return getSetMetadataMethodRecursively(clazz.getSuperclass());
        }

        return Optional.empty();
    }

    private Optional<Method> getSetMetadataMethod(Class<?> clazz) {
        // Searching for a method taking one argument of type MetadataDto which is assumed to be a setter for
        // the metadata object

        return setMetadataMethods.computeIfAbsent(clazz, this::getSetMetadataMethodRecursively);
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

    private Optional<URI> getMetadataIdForSingleObject(Object object) {

        return getMetadataIdAnnotatedFields(object.getClass()).stream()
                .findFirst()
                .map(field -> {
                    try {
                        return (URI) field.get(object);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private Set<URI> getMetadataIds(Object object) {
        // Searches recursively on the object for any metadata IDs (fields with @MetadataIdField) to request for

        final var idList = new HashSet<URI>();

        if (object instanceof Collection<?>) {

            return ((Collection<?>) object)
                    .stream()
                    .map(this::getMetadataIds)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }

        getMetadataIdForSingleObject(object).ifPresent(idList::add);


        getInjectMetadataAnnotatedFields(object.getClass()).forEach(field -> {
            try {
                idList.addAll(getMetadataIds(field.get(object)));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        return idList;
    }

    private void injectMetadataIntoDto(Object dto, Set<MetadataDto> metadataDtos) {
        // Searches for any field marked with @InjectMetadata and recursively invokes this method on those fields
        // including collections
        for (var field : getInjectMetadataAnnotatedFields(dto.getClass())) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                // Field is a collection, try to inject into all instances of this collection
                try {
                    ((Collection<?>) field.get(dto)).forEach(i -> injectMetadataIntoDto(i, metadataDtos));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    injectMetadataIntoDto(field.get(dto), metadataDtos);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Applies metadata to this object if a setMetadata method is found
        getSetMetadataMethod(dto.getClass()).ifPresent(setMetadata -> {
            getMetadataIdForSingleObject(dto).ifPresent(id -> {
                metadataDtos.stream()
                        .filter(metadataDto -> metadataDto.getPublicId().equals(id.toString()))
                        .forEach(metadataDto -> {
                            try {
                                setMetadata.setAccessible(true);
                                setMetadata.invoke(dto, metadataDto);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        });
            });

        });
    }

    private void injectMetadataIntoDtos(Object dtos, Set<MetadataDto> metadataDtos) {
        if (dtos instanceof Collection<?>) {
            ((Collection<?>) dtos).forEach(dto -> injectMetadataIntoDto(dto, metadataDtos));

            return;
        }

        // Single DTO
        injectMetadataIntoDto(dtos, metadataDtos);
    }

    @AfterReturning(value = "@annotation(no.ndla.taxonomy.service.InjectMetadata)", returning = "returnValue")
    public void injectMetadata(Object returnValue) {
        // First collect ALL ids we need to fetch metadata for
        final var idList = getMetadataIds(returnValue);

        // Fetch metadata for the IDs found
        final var metadata = metadataApiService.getMetadataByPublicId(idList);

        // Recursively inject metadata into the returning DTO
        injectMetadataIntoDtos(returnValue, metadata);
    }
}
