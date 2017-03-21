package technology.tabula;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.util.stream.Collectors.groupingBy;

@SuppressWarnings("serial")
public class TableWithRulingLines<T extends Rectangle> extends Table  {

    List<Ruling> verticalRulings, horizontalRulings;
    RectangleSpatialIndex<Cell> si = new RectangleSpatialIndex<Cell>();
    
    public TableWithRulingLines() {
        super();
    }

    public TableWithRulingLines(Rectangle area, Page page, List<Cell> cells,
            List<Ruling> horizontalRulings,
            List<Ruling> verticalRulings) {
        this();
        this.setRect(area);
        this.page = page;
        this.verticalRulings = verticalRulings;
        this.horizontalRulings = horizontalRulings;
        this.addCells(cells);
    }

    public static List<Cell> breakCells (List<Cell> cells) {

        return cells;
    }

    //Todo seems something goes wrong here; reassemble cells in this function
    private void addCells(List<Cell> cells) {
        List<Cell> new_cells=breakSpatialIndex(cells);


        List<List<Cell>> rowsOfCells = rowsOfCells(new_cells);
        for (int i = 0; i < rowsOfCells.size(); i++) {
            List<Cell> row = rowsOfCells.get(i);
            Iterator<Cell> rowCells = row.iterator();
            Cell cell = rowCells.next();
            List<List<Cell>> others = rowsOfCells(
                    si.contains(
                            new Rectangle(cell.getBottom(), si.getBounds().getLeft(), cell.getLeft() - si.getBounds().getLeft(), 
                                    si.getBounds().getBottom() - cell.getBottom())
                            ));
            int startColumn = 0;
            for (List<Cell> r: others) {
                startColumn = max(startColumn, r.size());
            }
            this.add(cell, i, startColumn++);
            while (rowCells.hasNext()) {
                this.add(rowCells.next(), i, startColumn++);
            }
        }
    }

    public List<Cell> breakSpatialIndex(List<Cell> cells){
        float minSize =2.5f;
        if (cells.isEmpty()) {
                return cells;
            }
            for (Cell ce: cells) {
                si.add(ce);
            }
//            List<Cell> cells =si.contains(si.getBounds());
        RectangleSpatialIndex<Cell> new_si = new RectangleSpatialIndex<Cell>();

        List<Cell> newCells=new ArrayList();
        float meanCellSize=cells.stream().map(c->c.width*c.height).reduce( 0.0f, (x,y) -> x+y)/cells.size();

        List<Cell> filteredCells=cells.stream().filter(c->(c.width*c.height>=max(meanCellSize/2,10))&&c.width>=minSize&&c.height>=minSize||c.getText()!="").collect(Collectors.toList());
        for (Cell cell:filteredCells){

            List<Cell> cellsInColumn = si.contains(new Rectangle(cell.getBottom(), cell.getLeft(), (float) cell.getWidth(),
                    si.getBounds().getBottom() - cell.getBottom()));

            List<Cell> cellsInRow = si.contains(new Rectangle(cell.getTop(), si.getBounds().getLeft(), (float) si.getBounds().getWidth(),
                    (float) cell.getHeight()));
            List<Cell> filteredCellsInColumn=cellsInColumn.stream().filter(c->((c.width*c.height>=max(meanCellSize/2,10))&&c.width>=minSize&&c.height>=minSize)||c.getText()!="").collect(Collectors.toList());
            List<Cell> filteredCellsInRow=cellsInRow.stream().filter(c->((c.width*c.height>=max(meanCellSize/2.,10))&&c.width>=minSize&&c.height>=minSize)||c.getText()!="").collect(Collectors.toList());
            List yCountList=filteredCellsInColumn.stream().map(c->c.y).collect(Collectors.toList());
            List xCountList=filteredCellsInRow.stream().map(c->c.x).collect(Collectors.toList());

            int maxCountY=getMaxCountValue(yCountList);
            int maxCountX=getMaxCountValue(xCountList);
            List<Cell> splitCells =  new ArrayList();
            for (int i=0;i<maxCountY;++i){
                for (int j=0;j<maxCountX;++j){
                    Cell temp_cell=new Cell(cell.getTop()+cell.height*j/maxCountX,cell.getLeft()+i*cell.width/maxCountY,cell.width/maxCountY,cell.height/maxCountX);
                    temp_cell.setTextElements(cell.getTextElements());
                    splitCells.add(temp_cell);
                }
            }
            newCells.addAll(splitCells);
            for (Cell ce: splitCells) {
                new_si.add(ce);
            }
        }

        si=new_si;
        return  newCells;

    }

    private static Integer getMaxCountValue(List myList){
        Collections.sort(myList, Collections.reverseOrder());
        int maxCount = 1;
        int currCount = 1;

        for (int i=1;i<myList.size();++i) {
            if (myList.get(i).equals(myList.get(i-1))) {
                ++currCount;
            } else {
                currCount = 1;
            }
            maxCount = Math.max(maxCount, currCount);


        }
        return maxCount;
    }



    private  List<List<Cell>> rowsOfCells(List<Cell> cells) {
        Cell c;
        float lastTop;
        List<List<Cell>> rv = new ArrayList<List<Cell>>();
        List<Cell> lastRow;
        
        if (cells.isEmpty()) {
            return rv;
        }

        //sort cells based on pixels
        Collections.sort(cells, new Comparator<Cell>() {
            @Override
            public int compare(Cell arg0, Cell arg1) {
                return java.lang.Double.compare(arg0.getTop(), arg1.getTop());
            }
        });

        Iterator<Cell> iter = cells.iterator();
        c = iter.next();
        //break cells here
        //find all cells in a same column with the target cell


        lastTop = (float) c.getTop();
        lastRow = new ArrayList<Cell>();
        lastRow.add(c);
        rv.add(lastRow);
        
        while (iter.hasNext()) {
            c = iter.next();
            //check if cell align this last cell(in a same row)
            //todo a new data structure to store cell
            if (!(Math.abs(c.getTop() - lastTop) < 2f)) {
                lastRow = new ArrayList<Cell>();
                rv.add(lastRow);
            }


            lastRow.add(c);
            lastTop = c.getTop();
        }
        //todo break the table down, generate a new table here



        return rv;
    }

    public static int maxCellsInListOfAxis( List<List<Cell>>  cells,int axis){
        int max_number =1;

        return 1;
    }
}
