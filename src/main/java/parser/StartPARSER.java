package parser;

import java.sql.Connection;
import java.sql.DriverManager;

public class StartPARSER {
    public static void main() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5433/", "", "");
        OsmPbfParser parser = new OsmPbfParser(conn);

        parser.parse("");

        if (parser.getStatistics().get("processed_objects") % 50_000 == 0) {
            parser.clearCaches();
        }

        parser.close();

    }
}
