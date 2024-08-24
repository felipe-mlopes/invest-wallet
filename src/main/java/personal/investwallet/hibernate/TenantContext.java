package personal.investwallet.hibernate;

public class TenantContext {
    private static final String DEFAULT_TENANT = "tenant01";
    private static final ThreadLocal<String> currentTenant = ThreadLocal.withInitial(() -> DEFAULT_TENANT);

    public static void setCurrentTenant(String tentant) {
        currentTenant.set(tentant);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
