package technology.tabula.writers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;

public class CSVWriter implements Writer {

    CSVPrinter printer;
    private boolean useLineReturns = true;

//    public CSVWriter() {
//        super();
//    }
//    
//    public CSVWriter(boolean useLineReturns) {
//        super();
//        this.useLineReturns = useLineReturns;
//    }

    void createWriter(Appendable out) {
        try {
            this.printer = new CSVPrinter(out, CSVFormat.EXCEL);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void write(Appendable out, Table table) throws IOException {

        this.createWriter(out);

        String caption= table.getCaption();
        //add caption in front of every table
        this.printer.printRecord(caption);

        for (List<RectangularTextContainer> row : table.getRows()) {
            List<String> cells = new ArrayList<String>(row.size());


            for (RectangularTextContainer tc : row) {

                cells.add(tc.getText());

            }
            this.printer.printRecord(cells);

        }
        this.printer.printRecord(" ");
        printer.flush();
    }

    @Override
    public void write(Appendable out, List<Table> tables) throws IOException {
        //todo csv writer multiple tables are overlapping
        for (Table table : tables) {
            write(out, table);
        }

    }


    // Write a single seer table body
    @Override
    public void write(Appendable out, ArrayList<String> table) throws IOException {
        this.printer = new CSVPrinter(out, CSVFormat.EXCEL);
        this.printer.printRecord(table);

        printer.flush();
    }
}



