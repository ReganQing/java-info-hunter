package com.ron.javainfohunter.api.exception;

/**
 * Resource Not Found Exception
 *
 * Thrown when a requested resource (e.g., RSS source, news, agent execution) cannot be found.
 * Results in HTTP 404 Not Found response when handled by GlobalExceptionHandler.
 *
 * @author JavaInfoHunter
 * @version 0.0.1-SNAPSHOT
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Construct with resource name and ID
     *
     * @param resourceName Name of the resource type (e.g., "RSS Source", "News")
     * @param id           ID of the resource
     */
    public ResourceNotFoundException(String resourceName, Long id) {
        super(formatMessage(resourceName, String.valueOf(id)));
    }

    /**
     * Construct with resource name and string ID
     *
     * @param resourceName Name of the resource type
     * @param id           ID of the resource as string
     */
    public ResourceNotFoundException(String resourceName, String id) {
        super(formatMessage(resourceName, id));
    }

    /**
     * Construct with custom message
     *
     * @param message Custom error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Format error message with resource name and ID
     *
     * @param resourceName Name of the resource type
     * @param id           ID of the resource
     * @return Formatted error message
     */
    private static String formatMessage(String resourceName, String id) {
        String name = (resourceName != null) ? resourceName : "Resource";
        return String.format("%s not found with id: %s", name, id);
    }
}
