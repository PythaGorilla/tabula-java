package technology.tabula.writers;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;


import technology.tabula.Table;
//import edu.psu.seersuite.extractors.tableextractor.model.Table;

public interface Writer {
    void write(Appendable out, Table table) throws IOException;
    void write(Appendable out, List<Table> tables) throws IOException;
    // csv writers for table-seer
    void write(Appendable out, ArrayList<String> seer_table) throws IOException;

}
