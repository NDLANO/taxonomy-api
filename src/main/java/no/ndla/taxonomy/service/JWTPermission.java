package no.ndla.taxonomy.service;

public class JWTPermission {
    private String api;
    private String environment;
    private String permission;

    public JWTPermission(String scope) {
        this.api = scope.split("-")[0].trim();
        this.environment = scope.split("-")[1].split(":")[0].trim();
        this.permission = scope.split(":")[1].trim();
    }

    public String getApi() {
        return api;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getPermission() {
        return permission;
    }

}
