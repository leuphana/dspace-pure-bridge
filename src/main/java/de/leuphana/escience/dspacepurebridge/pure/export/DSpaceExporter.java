package de.leuphana.escience.dspacepurebridge.pure.export;

import java.sql.SQLException;

import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import org.dspace.content.Item;
import org.dspace.core.Context;

public interface DSpaceExporter {
    void init() throws ApiException;

    void setupMappingFromConfiguration();

    void setupClassifications() throws ApiException;

    ExportItem createExport(Context context, Item item, String syncTypeValue) throws
        ApiException,
        SQLException;

    ExportResult export(ExportItem exportItem) throws ApiException;

    DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi,
                                                                   String title);

}
