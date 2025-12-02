package de.leuphana.escience.dspacepurebridge;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;

import java.sql.SQLException;

public class CLIScriptContextUtils {

    public static final String DISPATCHER_SET_NAME = "dspacePureBridge";

    public static void setEPerson(Context context) throws SQLException {
        EPerson myEPerson =
            EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, System.getenv("ADMIN_EMAIL"));
        if (myEPerson == null) {
            throw new UnsupportedOperationException("EPerson cannot be found: ensure environment variable 'ADMIN_EMAIL' is configured properly!");
        }
        context.setCurrentUser(myEPerson);
    }

    private CLIScriptContextUtils() {
    }

    public static Context createReducedContext() throws SQLException {
        Context context = new Context();
        CLIScriptContextUtils.setEPerson(context);
        context.setDispatcher(DISPATCHER_SET_NAME);
        context.turnOffAuthorisationSystem();
        return context;
    }

    public static void closeContext(Context context) {
        if (context != null && context.isValid()) {
            try {
                context.dispatchEvents();
                context.complete();
            } catch (Exception e) {
                context.abort();
                throw new RuntimeException(e);
            }
        }
    }
}
