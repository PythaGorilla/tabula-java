package technology.tabula;

import edu.psu.seersuite.extractors.tableextractor.extraction.IPdfParser;
import edu.psu.seersuite.extractors.tableextractor.extraction.PdfBoxParser;
import org.apache.commons.cli.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.detectors.DetectionAlgorithm;
import technology.tabula.detectors.NurminenDetectionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.kotlin.KutilsKt;
import technology.tabula.writers.CSVWriter;
import technology.tabula.writers.JSONWriter;
import technology.tabula.writers.TSVWriter;
import technology.tabula.writers.Writer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static technology.tabula.kotlin.KutilsKt.meta2table;


public class CommandLineApp {

    private static String VERSION = "0.9.2";
    private static String VERSION_STRING = String.format("tabula %s (c) 2012-2016 Manuel Aristarán", VERSION);
    private static String BANNER = "\nTabula helps you extract tables from PDFs\n\n";

    private Appendable defaultOutput;
    private Rectangle pageArea;
    private List<Integer> pages;
    private OutputFormat outputFormat;
    private String password;
    private TableExtractor tableExtractor;

    public CommandLineApp(Appendable defaultOutput, CommandLine line) throws ParseException {
        this.defaultOutput = defaultOutput;
        this.pageArea = CommandLineApp.whichArea(line);
        this.pages = CommandLineApp.whichPages(line);
        this.outputFormat = CommandLineApp.whichOutputFormat(line);
        this.tableExtractor = CommandLineApp.createExtractor(line);

        if (line.hasOption('s')) {
          this.password = line.getOptionValue('s');
        }
    }

    public static void main(String[] args) {
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(buildOptions(), args );

            if (line.hasOption('h')) {
                printHelp();
                System.exit(0);
            }

            if (line.hasOption('v')) {
                System.out.println(VERSION_STRING);
                System.exit(0);
            }

            new CommandLineApp(System.out, line).extractTables(line);
        } catch(ParseException exp) {
            System.err.println("Error: " + exp.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

    public void extractTables(CommandLine line) throws ParseException {
        if (line.hasOption('b')) {
          if (line.getArgs().length != 0) {
              throw new ParseException("Filename specified with batch\nTry --help for help");
          }

          File pdfDirectory = new File(line.getOptionValue('b'));
          if (!pdfDirectory.isDirectory()) {
            throw new ParseException("Directory does not exist or is not a directory");
          }
          extractDirectoryTables(line, pdfDirectory);
          return;
        }

        if (line.getArgs().length != 1) {
            throw new ParseException("Need one filename\nTry --help for help");
        }

        File pdfFile = new File(line.getArgs()[0]);
        if (!pdfFile.exists()) {
            throw new ParseException("File does not exist");
        }
        extractFileTables(line, pdfFile);
    }

    public void extractDirectoryTables(CommandLine line, File pdfDirectory) throws ParseException {
        File[] pdfs = pdfDirectory.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".pdf");
          }
        });

        for (File pdfFile : pdfs) {
          File outputFile = new File(getOutputFilename(pdfFile));
          extractFileInto(pdfFile, outputFile);
        }
    }

    public void extractFileTables(CommandLine line, File pdfFile) throws ParseException {
        Appendable outFile = this.defaultOutput;
        if (!line.hasOption('o')) {
          extractFile(pdfFile, this.defaultOutput);
          return;
        }

        File outputFile = new File(line.getOptionValue('o'));
        extractFileInto(pdfFile, outputFile);
    }

    public void extractFileInto(File pdfFile, File outputFile) throws ParseException {
        BufferedWriter bufferedWriter = null;
        try {
            FileWriter fileWriter = new FileWriter(outputFile.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);

            outputFile.createNewFile();
            extractFile(pdfFile, bufferedWriter);
        } catch (IOException e) {
            throw new ParseException("Cannot create file " + outputFile);
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    System.out.println("Error in closing the BufferedWriter" + e);
                }
            }
        }
    }

    private void extractFile(File pdfFile, Appendable outFile) throws ParseException {
        PDDocument pdfDocument = null;
        try {
            pdfDocument = PDDocument.load(pdfFile);
            PageIterator pageIterator = getPageIterator(pdfDocument);
            List<Table> tables = new ArrayList<Table>();

            while (pageIterator.hasNext()) {
                Page page = pageIterator.next();

                if (pageArea != null) {
                    page = page.getArea(pageArea);
                }

                tables.addAll(tableExtractor.extractTables(page));
            }

            List new_tables=getTableMeta(tables,pdfFile);

            writeTables(new_tables, outFile);

        } catch (IOException e) {
            throw new ParseException(e.getMessage());
        } finally {
          try {
              if (pdfDocument != null) {
                pdfDocument.close();
              }
          } catch (IOException e) {
              System.out.println("Error in closing pdf document" + e);
          }
        }
    }

    private PageIterator getPageIterator(PDDocument pdfDocument) throws IOException {
        ObjectExtractor extractor = (this.password == null) ?
                new ObjectExtractor(pdfDocument) :
                new ObjectExtractor(pdfDocument, this.password);
        PageIterator pageIterator = (pages == null) ?
          extractor.extract() :
          extractor.extract(pages);
        return pageIterator;
    }

    // CommandLine parsing methods

    private static OutputFormat whichOutputFormat(CommandLine line) throws ParseException {
        if (!line.hasOption('f')) {
            return OutputFormat.CSV;
        }

        try {
            return OutputFormat.valueOf(line.getOptionValue('f'));
        } catch (IllegalArgumentException e) {
            throw new ParseException(String.format(
                    "format %s is illegal. Available formats: %s",
                    line.getOptionValue('f'),
                    Utils.join(",", OutputFormat.formatNames())));
        }
    }

    private static Rectangle whichArea(CommandLine line) throws ParseException {
        if (!line.hasOption('a')) {
          return null;
        }

        List<Float> f = parseFloatList(line.getOptionValue('a'));
        if (f.size() != 4) {
            throw new ParseException("area parameters must be top,left,bottom,right");
        }
        return new Rectangle(f.get(0), f.get(1), f.get(3) - f.get(1), f.get(2) - f.get(0));
    }

    private static List<Integer> whichPages(CommandLine line) throws ParseException {
        String pagesOption = line.hasOption('p') ? line.getOptionValue('p') : "1";
        return Utils.parsePagesOption(pagesOption);
    }

    private static ExtractionMethod whichExtractionMethod(CommandLine line) {
        // -r/--spreadsheet [deprecated; use -l] or -l/--lattice
        if (line.hasOption('r') || line.hasOption('l')) {
            return ExtractionMethod.SPREADSHEET;
        }

        // -n/--no-spreadsheet [deprecated; use -t] or  -c/--columns or -g/--guess or -t/--stream //deleted guess from this
        if (line.hasOption('n') || line.hasOption('c')  || line.hasOption('t')) {
            return ExtractionMethod.BASIC;
        }
        return ExtractionMethod.DECIDE;
    }

    private static TableExtractor createExtractor(CommandLine line) throws ParseException {
      TableExtractor extractor = new TableExtractor();
      extractor.setGuess(line.hasOption('g'));
      extractor.setMethod(CommandLineApp.whichExtractionMethod(line));
      extractor.setUseLineReturns(line.hasOption('u'));

      if (line.hasOption('c')) {
          extractor.setVerticalRulingPositions(parseFloatList(line.getOptionValue('c')));
      }
      return extractor;
    }

    // utilities, etc.

    public static List<Float> parseFloatList(String option) throws ParseException {
        String[] f = option.split(",");
        List<Float> rv = new ArrayList<Float>();
        try {
            for (int i = 0; i < f.length; i++) {
                rv.add(Float.parseFloat(f[i]));
            }
            return rv;
        } catch (NumberFormatException e) {
            throw new ParseException("Wrong number syntax");
        }
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("tabula", BANNER, buildOptions(), "", true);
    }

    @SuppressWarnings("static-access")
    public static Options buildOptions() {
        Options o = new Options();

        o.addOption("v", "version", false, "Print version and exit.");
        o.addOption("h", "help", false, "Print this help text.");
        o.addOption("g", "guess", false, "Guess the portion of the page to analyze per page.");
        o.addOption("d", "debug", false, "Print detected table areas instead of processing");
        o.addOption("r", "spreadsheet", false, "[Deprecated in favor of -l/--lattice] Force PDF to be extracted using spreadsheet-style extraction (if there are ruling lines separating each cell, as in a PDF of an Excel spreadsheet)");
        o.addOption("n", "no-spreadsheet", false, "[Deprecated in favor of -t/--stream] Force PDF not to be extracted using spreadsheet-style extraction (if there are no ruling lines separating each cell)");
        o.addOption("l", "lattice", false, "Force PDF to be extracted using lattice-mode extraction (if there are ruling lines separating each cell, as in a PDF of an Excel spreadsheet)");
        o.addOption("t", "stream", false, "Force PDF to be extracted using stream-mode extraction (if there are no ruling lines separating each cell)");
        o.addOption("i", "silent", false, "Suppress all stderr output.");
        o.addOption("u", "use-line-returns", false, "Use embedded line returns in cells. (Only in spreadsheet mode.)");
        o.addOption("d", "debug", false, "Print detected table areas instead of processing.");
        o.addOption(OptionBuilder.withLongOpt("batch")
            .withDescription("Convert all .pdfs in the provided directory.")
            .hasArg()
            .withArgName("DIRECTORY")
            .create("b"));
        o.addOption(OptionBuilder.withLongOpt("outfile")
                                 .withDescription("Write output to <file> instead of STDOUT. Default: -")
                                 .hasArg()
                                 .withArgName("OUTFILE")
                                 .create("o"));
        o.addOption(OptionBuilder.withLongOpt("format")
                                 .withDescription("Output format: (" + Utils.join(",", OutputFormat.formatNames()) + "). Default: CSV")
                                 .hasArg()
                                 .withArgName("FORMAT")
                                 .create("f"));
        o.addOption(OptionBuilder.withLongOpt("password")
                                 .withDescription("Password to decrypt document. Default is empty")
                                 .hasArg()
                                 .withArgName("PASSWORD")
                                 .create("s"));
        o.addOption(OptionBuilder.withLongOpt("columns")
                                 .withDescription("X coordinates of column boundaries. Example --columns 10.1,20.2,30.3")
                                 .hasArg()
                                 .withArgName("COLUMNS")
                                 .create("c"));
        o.addOption(OptionBuilder.withLongOpt("area")
                                 .withDescription("Portion of the page to analyze (top,left,bottom,right). Example: --area 269.875,12.75,790.5,561. Default is entire page")
                                 .hasArg()
                                 .withArgName("AREA")
                                 .create("a"));
        o.addOption(OptionBuilder.withLongOpt("pages")
                                 .withDescription("Comma separated list of ranges, or all. Examples: --pages 1-3,5-7, --pages 3 or --pages all. Default is --pages 1")
                                 .hasArg()
                                 .withArgName("PAGES")
                                 .create("p"));

        return o;
    }

    private static class TableExtractor {
      private boolean guess = false;
      private boolean useLineReturns = false;
      private BasicExtractionAlgorithm basicExtractor = new BasicExtractionAlgorithm();
      private SpreadsheetExtractionAlgorithm spreadsheetExtractor = new SpreadsheetExtractionAlgorithm();
      private List<Float> verticalRulingPositions = null;
      private ExtractionMethod method = ExtractionMethod.BASIC;

      public TableExtractor() {
      }

      public void setVerticalRulingPositions(List<Float> positions) {
        this.verticalRulingPositions = positions;
      }

      public void setGuess(boolean guess) {
        this.guess = guess;
      }

      public void setUseLineReturns(boolean useLineReturns) {
        this.useLineReturns = useLineReturns;
      }

      public void setMethod(ExtractionMethod method) {
        this.method = method;
      }

      public List<Table> extractTables(Page page) {
          ExtractionMethod effectiveMethod = this.method;
          if (effectiveMethod == ExtractionMethod.DECIDE) {
            effectiveMethod = spreadsheetExtractor.isTabular(page) ?
              ExtractionMethod.SPREADSHEET :
              ExtractionMethod.BASIC;
          }
          switch(effectiveMethod) {
          case BASIC:
              return extractTablesBasic(page);
          case SPREADSHEET:
              return extractTablesSpreadsheet(page);
          default:
            return new ArrayList<Table>();
          }
      }

      public List<Table> extractTablesBasic(Page page) {
        if (guess) {
          // guess the page areas to extract using a detection algorithm
          // currently we only have a detector that uses spreadsheets to find table areas
          DetectionAlgorithm detector = new NurminenDetectionAlgorithm();
          List<Rectangle> guesses = detector.detect(page);
          List<Table> tables = new ArrayList<Table>();

          for (Rectangle guessRect : guesses) {
              Page guess = page.getArea(guessRect);
              tables.addAll(basicExtractor.extract(guess));
          }
          return tables;
        }

        if (verticalRulingPositions != null) {
          return basicExtractor.extract(page, verticalRulingPositions);
        }
        //find vertical rulings if there's any from page
        basicExtractor.verticalRulings=page.getVerticalRulings();
        return basicExtractor.extract(page);
      }

      public List<Table> extractTablesSpreadsheet(Page page) {
          // TODO add useLineReturns
          return (List<Table>)spreadsheetExtractor.extract(page);
      }
    }

    private void writeTables(List<Table> tables, Appendable out) throws IOException {
        Writer writer = null;
        switch (outputFormat) {
        case CSV:
            writer = new CSVWriter();
            break;
        case JSON:
            writer = new JSONWriter();
            break;
        case TSV:
            writer = new TSVWriter();
            break;
        }
        writer.write(out, tables);
    }

    private String getOutputFilename(File pdfFile) {
        String extension = ".csv";
        switch (outputFormat) {
        case CSV:
            extension = ".csv";
            break;
        case JSON:
            extension = ".json";
            break;
        case TSV:
            extension = ".tsv";
            break;
        }
        return pdfFile.getPath().replaceFirst("(\\.pdf|)$", extension);
    }

    private List getTableMeta(List<Table> tables, File pdfFile){


        //get caption and reftext from table_seer
        edu.psu.seersuite.extractors.tableextractor.extraction.TableExtractor seer_extractor= new edu.psu.seersuite.extractors.tableextractor.extraction.TableExtractor();
        IPdfParser seer_parser = null;

        try {
            seer_parser = new PdfBoxParser();
        } catch (IOException e) {
            e.printStackTrace();
        }
        seer_extractor.setParser(seer_parser);
        //attach table meta data to its page in a map
        List<edu.psu.seersuite.extractors.tableextractor.model.Table> seer_tables=seer_extractor.extract(pdfFile,"C:/");
        List<Integer> pageNumList=seer_tables.stream().map(e -> e.getPageNumber()).collect(Collectors.toList());
        List captionList=seer_tables.stream().map(e -> e.getCaption()).collect(Collectors.toList());
        List reftxtList=seer_tables.stream().map(e -> e.getRefTextList()).collect(Collectors.toList());
        List meta_list=KutilsKt.zip2pairs(captionList,reftxtList);
        //List list2=KutilsKt.flatZip(meta_list,reftxtList);
        Map meta_map= KutilsKt.zip2map(pageNumList,meta_list);
        Map table_map=KutilsKt.zip2map(tables.stream().map(e->e.getPageNo()).collect(Collectors.toList()),tables );


        List new_table_list=meta2table(meta_map,table_map);

        return new_table_list;

    }

    private enum OutputFormat {
        CSV,
        TSV,
        JSON;

        static String[] formatNames() {
            OutputFormat[] values = OutputFormat.values();
            String[] rv = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                rv[i] = values[i].name();
            }
            return rv;
        }
    }

    private enum ExtractionMethod {
        BASIC,
        SPREADSHEET,
        DECIDE
    }

    private class DebugOutput {
        private boolean debugEnabled;

        public DebugOutput(boolean debug) {
            this.debugEnabled = debug;
        }

        public void debug(String msg) {
            if (this.debugEnabled) {
                System.err.println(msg);
            }
        }
    }

}
