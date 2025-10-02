/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;

public class CLIScriptContextUtils {

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

    public static Context createContext(String dispatcher) throws SQLException {
        Context context = new Context();
        CLIScriptContextUtils.setEPerson(context);
        context.setDispatcher(dispatcher);
        context.turnOffAuthorisationSystem();
        return context;
    }

    public static Context createReducedContext() throws SQLException {
        return createContext("reduced");
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
