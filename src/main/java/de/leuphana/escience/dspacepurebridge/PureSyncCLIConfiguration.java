package de.leuphana.escience.dspacepurebridge;

import org.apache.commons.cli.Options;

public class PureSyncCLIConfiguration {

    static Options getOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("i", "import", false, "import data from pure");
        options.addOption("e", "export", false, "export data to pure");
        options.addOption("l", "exportLimit", true, "stop export after specified number of successful exports");
        options.addOption("c", "checkOnly", false, "perform no export, only verify export validity of items");
        options.addOption("x", "exportHandle", true, "item handle to be exported to pure");
        return options;
    }
}

