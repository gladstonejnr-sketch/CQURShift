package secureshift;

import secureshift.data.*;
import secureshift.service.ShiftService;
import secureshift.service.GuardService;
import secureshift.service.SiteService;

/**
 * Application configuration class.
 * Now uses JDBC repositories for database persistence.
 */
public class AppConfig {

    // JDBC Repository instances
    private static final GuardRepositoryJDBC guardRepository = new GuardRepositoryJDBC();
    private static final ShiftRepositoryJDBC shiftRepository = new ShiftRepositoryJDBC();
    private static final SiteRepositoryJDBC siteRepository = new SiteRepositoryJDBC();

    // Service instances ✅ Fixed variable names to match declarations above
    private static final ShiftService shiftService =
            new ShiftService(shiftRepository, guardRepository, siteRepository);
    private static final GuardService guardService =
            new GuardService(guardRepository);
    private static final SiteService siteService =
            new SiteService(siteRepository);

    public static ShiftService getShiftService() {
        return shiftService;
    }

    public static GuardService getGuardService() {
        return guardService;
    }

    public static SiteService getSiteService() {
        return siteService;
    }

    /**
     * Test database connection on startup.
     */
    static {
        System.out.println("🔌 Testing database connection...");
        if (DatabaseConfig.testConnection()) {
            System.out.println("✅ Database connection successful");
        } else {
            System.err.println("⚠️  Database connection failed - using in-memory fallback?");
        }
    }
}


